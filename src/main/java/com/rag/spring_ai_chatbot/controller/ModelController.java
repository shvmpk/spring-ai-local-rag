package com.rag.spring_ai_chatbot.controller;

import com.rag.spring_ai_chatbot.model.ApiResponse;
import com.rag.spring_ai_chatbot.service.ModelManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Model", description = "Model management endpoints")
@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelManagementService modelService;

    @PostMapping("/install")
    public ResponseEntity<ApiResponse<String>> install(@RequestParam String model) {
        modelService.scheduleInstall(model);
        return ResponseEntity.accepted().body(ApiResponse.ok("Install scheduled"));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String,Object>>> status(@RequestParam String model) {
        int prog = modelService.getProgress(model);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("model", model, "progress", prog, "installing", prog>=0 && prog<100)));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<String>> cancel(@RequestParam String model) {
        modelService.cancelInstall(model);
        return ResponseEntity.ok(ApiResponse.ok("Cancel requested"));
    }

    @GetMapping("/installed")
    public ResponseEntity<ApiResponse<Boolean>> isInstalled(@RequestParam String model) {
        return ResponseEntity.ok(ApiResponse.ok(modelService.isModelInstalled(model)));
    }
}