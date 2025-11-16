package com.rag.spring_ai_chatbot.model;

public record ApiResponse<T>(boolean success, T data, String error) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, data, null); }
    public static <T> ApiResponse<T> error(String err) { return new ApiResponse<>(false, null, err); }
}
