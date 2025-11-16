package com.rag.spring_ai_chatbot.controller;

import com.rag.spring_ai_chatbot.model.ApiResponse;
import com.rag.spring_ai_chatbot.model.ChatResponse;
import com.rag.spring_ai_chatbot.model.HumanMessage;
import com.rag.spring_ai_chatbot.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chat", description = "Chat endpoint")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Ask a question", description = "Sends a query to the LLM")
    @PostMapping("/inference")
    public ResponseEntity<ApiResponse<ChatResponse>> infer(@RequestBody HumanMessage req) {
        try {
            String answer = chatService.infer(req);
            return ResponseEntity.ok(ApiResponse.ok(new ChatResponse(req.sessionId(), answer, req.modelName())));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Inference failed"));
        }
    }
}

