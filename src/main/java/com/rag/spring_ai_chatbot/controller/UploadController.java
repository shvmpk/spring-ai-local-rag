package com.rag.spring_ai_chatbot.controller;

import com.rag.spring_ai_chatbot.etl.EtlMessagePublisher;
import com.rag.spring_ai_chatbot.model.ApiResponse;
import com.rag.spring_ai_chatbot.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Tag(name = "Upload", description = "Upload endpoint")
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final FileStorageService storage;
    private final EtlMessagePublisher publisher;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> upload(
            @RequestParam("files") List<MultipartFile> files) {

        try {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    Path saved = storage.save(file);
                    publisher.publishFile(saved);
                }
            }
            return ResponseEntity.accepted()
                    .body(ApiResponse.ok("Files uploaded and queued"));
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }
}
