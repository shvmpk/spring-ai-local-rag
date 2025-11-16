package com.rag.spring_ai_chatbot.model;

import java.util.UUID;

public record HumanMessage(String query, String sessionId, String modelName) {
    public HumanMessage {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("query cannot be empty");
        if (sessionId == null || sessionId.isBlank()) sessionId = UUID.randomUUID().toString();
    }
}

