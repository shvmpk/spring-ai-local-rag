package com.rag.spring_ai_chatbot.controller;

import com.rag.spring_ai_chatbot.model.ApiResponse;
import com.rag.spring_ai_chatbot.service.ChatMemory;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Session", description = "Session endpoint")
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final ChatMemory chatMemory;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> history(@PathVariable String id,
                                                  @RequestParam(defaultValue = "0") int offset,
                                                  @RequestParam(defaultValue = "20") int limit) {
        List<String> messages = chatMemory.getPaginated(id, offset, limit);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "offset", offset,
                "limit", limit,
                "messages", messages
        )));
    }
}