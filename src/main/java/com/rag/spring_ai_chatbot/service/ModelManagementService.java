package com.rag.spring_ai_chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class ModelManagementService {

    private final WebClient webClient;
    private final StringRedisTemplate redis;
    private final ThreadPoolTaskExecutor installExecutor;
    private final ObjectMapper objectMapper;
    private final String installPrefix = "model:install:";

    // track handles so we can cancel
    private final Map<String, Process> processMap = new ConcurrentHashMap<>();
    private final Map<String, Disposable> disposableMap = new ConcurrentHashMap<>();

    public ModelManagementService(WebClient ollamaWebClient,
                                  StringRedisTemplate redisTemplate,
                                  ThreadPoolTaskExecutor modelInstallExecutor) {
        this.webClient = ollamaWebClient;
        this.redis = redisTemplate;
        this.installExecutor = modelInstallExecutor;
        this.objectMapper = new ObjectMapper();
    }

    public void scheduleInstall(String model) {
        String key = installPrefix + model;
        redis.opsForValue().set(key, "0");
        installExecutor.submit(() -> doInstallHttpFirst(model, key));
    }

    private void doInstallHttpFirst(String model, String key) {
        // try HTTP streaming install via /api/pull
        AtomicBoolean success = new AtomicBoolean(false);
        try {
            Disposable d = webClient.post()
                    .uri("/api/pull")
                    .bodyValue(Map.of("name", model))
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMinutes(30))
                    .doOnNext(line -> parseAndStoreProgress(line, key))
                    .doOnError(err -> log.warn("HTTP pull error: {}", err.getMessage()))
                    .doOnComplete(() -> success.set(true))
                    .subscribe();

            // store disposable for cancellation
            disposableMap.put(key, d);

            // wait until complete or timeout — but subscription will run on reactive threads
            int waited = 0;
            while (!success.get() && waited < 60 * 30) {
                if (redis.opsForValue().get(key) != null && redis.opsForValue().get(key).equals("-2")) {
                    // cancelled flag set
                    d.dispose();
                    disposableMap.remove(key);
                    redis.opsForValue().set(key, "-2");
                    log.info("Install cancelled (HTTP) for {}", model);
                    return;
                }
                TimeUnit.SECONDS.sleep(1);
                waited++;
            }

            if (success.get()) {
                redis.opsForValue().set(key, "100");
                disposableMap.remove(key);
                log.info("Model {} installed via HTTP", model);
                return;
            } else {
                // timeout — fallthrough to shell
                d.dispose();
                disposableMap.remove(key);
                log.warn("HTTP install timed out for {}, falling back to shell", model);
            }
        } catch (Exception e) {
            log.warn("HTTP install failed for {}: {}", model, e.getMessage());
        }

        // fallback to shell-based install
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-lc", "ollama pull " + model);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            processMap.put(key, p);

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    parseAndStoreProgress(line, key);
                    // check cancellation
                    if ("-2".equals(redis.opsForValue().get(key))) {
                        p.destroyForcibly();
                        processMap.remove(key);
                        redis.opsForValue().set(key, "-2");
                        log.info("Install cancelled (process) for {}", model);
                        return;
                    }
                }
            }
            int exit = p.waitFor();
            processMap.remove(key);
            if (exit == 0) {
                redis.opsForValue().set(key, "100");
            } else {
                redis.opsForValue().set(key, "-1");
            }
        } catch (Exception e) {
            log.error("Process-based install failed: {}", e.getMessage(), e);
            redis.opsForValue().set(key, "-1");
        }
    }

    private void parseAndStoreProgress(String line, String key) {
        if (line == null) return;
        try {
            JsonNode node = objectMapper.readTree(line);
            if (node.has("completed") && node.has("total") && node.get("total").asLong() > 0) {
                double p = (double) node.get("completed").asLong() / node.get("total").asLong() * 100;
                redis.opsForValue().set(key, String.valueOf((int) p));
                return;
            }
            // Fallback for other cases
            String text = node.toString();
            if (text.matches(".*\\b\\d{1,3}%.*")) {
                String pct = text.replaceAll(".*?(\\d{1,3})%.*", "$1");
                redis.opsForValue().set(key, pct);
            }
        } catch (Exception ex) {
            // Raw text for shell
            if (line.matches(".*\\b\\d{1,3}%.*")) {
                String pct = line.replaceAll(".*?(\\d{1,3})%.*", "$1");
                redis.opsForValue().set(key, pct);
            }
        }
    }

    public int getProgress(String model) {
        String v = redis.opsForValue().get(installPrefix + model);
        if (v == null) return 0;
        try { return Integer.parseInt(v); } catch (Exception ex) { return -1; }
    }

    public void cancelInstall(String model) {
        String key = installPrefix + model;
        redis.opsForValue().set(key, "-2");
        // best-effort cancel for HTTP disposable
        Disposable d = disposableMap.remove(key);
        if (d != null && !d.isDisposed()) d.dispose();

        Process p = processMap.remove(key);
        if (p != null && p.isAlive()) p.destroyForcibly();
    }

    public boolean isModelInstalled(String model) {
        try {
            String resp = webClient.get().uri("/api/tags").retrieve().bodyToMono(String.class).block(Duration.ofSeconds(5));
            if (resp != null) {
                // Better: Parse JSON
                JsonNode root = objectMapper.readTree(resp);
                JsonNode models = root.get("models");
                if (models != null && models.isArray()) {
                    for (JsonNode m : models) {
                        if (model.equals(m.get("name").asText())) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        // Fallback shell: Fix to use curl or parse table
        try {
            Process p = new ProcessBuilder("ollama", "list").start();  // No bash -lc needed?
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();
            // out is table: skip header, check if line starts with model
            String[] lines = out.split("\n");
            for (int i = 1; i < lines.length; i++) {  // Skip header
                if (lines[i].trim().startsWith(model)) return true;
            }
        } catch (Exception e) { /* ignore */ }
        return false;
    }
}