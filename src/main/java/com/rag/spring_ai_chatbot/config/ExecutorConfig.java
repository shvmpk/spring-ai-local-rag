package com.rag.spring_ai_chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean("etlTaskExecutor")
    public ThreadPoolTaskExecutor etlTaskExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(4);
        t.setMaxPoolSize(16);
        t.setQueueCapacity(2000);
        t.setThreadNamePrefix("etl-worker-");
        t.initialize();
        return t;
    }

    @Bean("modelInstallExecutor")
    public ThreadPoolTaskExecutor modelInstallExecutor() {
        ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
        t.setCorePoolSize(2);
        t.setMaxPoolSize(6);
        t.setQueueCapacity(50);
        t.setThreadNamePrefix("model-install-");
        t.initialize();
        return t;
    }
}