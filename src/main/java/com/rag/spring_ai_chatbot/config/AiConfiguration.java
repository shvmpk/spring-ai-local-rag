package com.rag.spring_ai_chatbot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import com.rag.spring_ai_chatbot.service.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiConfiguration {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaApiEndpoint;

    @Value("${spring.ai.ollama.chat.options.model}")
    private String chatModel;

    @Value("${spring.ai.vectorstore.chroma.client.host}:${spring.ai.vectorstore.chroma.client.port}")
    private String chromaUrl;

    @Bean
    public WebClient ollamaWebClient() {
        return WebClient.builder().baseUrl(ollamaApiEndpoint).build();
    }

    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi(ollamaApiEndpoint);
    }

    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder().model(chatModel).temperature(0.0).build())
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel(OllamaApi ollamaApi) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder().model(OllamaModel.NOMIC_EMBED_TEXT).build())
                .build();
    }

    @Bean
    public ChromaApi chromaApi(RestClient.Builder restClientBuilder) {
        return new ChromaApi(chromaUrl, restClientBuilder);
    }

    @Bean
    public ChromaVectorStore chromaVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("rag")
                .initializeSchema(true)
                .build();
    }

    @Bean
    public ChatClient ollamaChatClient(
            OllamaChatModel ollamaChatModel,
            ChatMemory chatMemory,
            ChromaVectorStore vectorStore
    ) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem("""
                    You are a helpful assistant. Prefer to answer using the provided information.
                    If the answer is not in the context, tell the user about it then use your own knowledge.
                    """)
                .defaultAdvisors(
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.builder().similarityThreshold(0.5d).build()
                        )
                )
                .build();
    }
}