package com.rag.spring_ai_chatbot.model;

public record ChatResponse(String sessionId, String answer, String model) {}