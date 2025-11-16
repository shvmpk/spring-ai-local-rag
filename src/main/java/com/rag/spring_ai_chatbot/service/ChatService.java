package com.rag.spring_ai_chatbot.service;

import com.rag.spring_ai_chatbot.model.HumanMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.client.ChatClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String infer(HumanMessage req) {
        String model = (req.modelName() == null || req.modelName().isBlank())
                ? "qwen2.5-coder:14b" : req.modelName();

        // Load previous history (excluding current query)
        List<String> rawHistory = chatMemory.getPaginated(req.sessionId(), 0, 200); // MAX_HISTORY
        List<Message> history = new ArrayList<>();
        for (String raw : rawHistory) {
            try {
                JsonNode node = objectMapper.readTree(raw);
                String role = node.get("role").asText();
                String text = node.get("text").asText();
                if ("user".equals(role)) {
                    history.add(new UserMessage(text));
                } else if ("assistant".equals(role)) {
                    history.add(new AssistantMessage(text));
                }
            } catch (Exception e) {
                log.warn("Failed to parse history message: {}", raw);
            }
        }

        // Now call with history + current user query
        String answer = this.chatClient
                .prompt()
                .messages(history) // Previous messages
                .user(req.query()) // Current user message (used for RAG advisor and prompt)
                .options(ChatOptions.builder().model(model).build())
                .call()
                .content();

        chatMemory.add(req.sessionId(), "user", req.query());
        chatMemory.add(req.sessionId(), "assistant", answer);
        return answer;
    }
}