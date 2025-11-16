package com.rag.spring_ai_chatbot.service;

import java.util.List;

public interface ChatMemory {
    void add(String sessionId, String role, String text);
    /**
     * Paginated read: offset=0 means earliest message (chronological).
     */
    List<String> getPaginated(String sessionId, int offset, int limit);
}
