package com.rag.spring_ai_chatbot.storage;

import com.rag.spring_ai_chatbot.service.ChatMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redis;
    private static final int MAX_HISTORY = 200;

    private String key(String sessionId) { return "chat:history:" + sessionId; }

    @Override
    public void add(String sessionId, String role, String text) {
        long ts = Instant.now().toEpochMilli();
        String payload = "{\"ts\":" + ts + ",\"role\":\"" + role + "\",\"text\":\"" + escape(text) + "\"}";
        // Use RPush to append at the end so chronological order is preserved
        redis.opsForList().rightPush(key(sessionId), payload);
        // trim to keep latest MAX_HISTORY
        redis.opsForList().trim(key(sessionId), -MAX_HISTORY, -1);
        redis.expire(key(sessionId), Duration.ofDays(7));
    }

    @Override
    public List<String> getPaginated(String sessionId, int offset, int limit) {
        if (limit <= 0) limit = MAX_HISTORY;
        // LRANGE uses 0-based index; get chronological slice
        long start = offset;
        long end = offset + limit - 1;
        List<String> raw = redis.opsForList().range(key(sessionId), start, end);
        if (raw == null) return List.of();
        return raw.stream().collect(Collectors.toList());
    }

    private String escape(String s) { return s == null ? "" : s.replace("\"","\\\"").replace("\n","\\n"); }
}