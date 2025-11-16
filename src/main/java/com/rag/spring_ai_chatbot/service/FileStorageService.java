package com.rag.spring_ai_chatbot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {
    private final Path root = Paths.get(System.getProperty("user.dir"), "data", "uploads")
            .toAbsolutePath().normalize();

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(root);
        log.info("Upload directory ensured at: {}", root);
    }

    public Path save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String filename = originalFilename != null
                    ? originalFilename
                    : "uploaded-file-" + System.currentTimeMillis();

            String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String uniqueName = UUID.randomUUID() + "-" + safeName;
            Path destination = root.resolve(uniqueName);

            file.transferTo(destination.toFile());

            log.info("File saved: {}", destination);
            return destination;

        } catch (IOException e) {
            log.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}
