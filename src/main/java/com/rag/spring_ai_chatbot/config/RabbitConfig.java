package com.rag.spring_ai_chatbot.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ETL_QUEUE = "etl.files";
    public static final String ETL_EXCHANGE = "etl.exchange";
    public static final String ETL_ROUTING_KEY = "etl.files.key";
    public static final String ETL_FAILED_QUEUE = "etl.files.failed";

    @Bean
    public DirectExchange etlExchange() {
        return new DirectExchange(ETL_EXCHANGE, true, false);
    }

    @Bean
    public Queue etlQueue() {
        return QueueBuilder.durable(ETL_QUEUE).build();
    }

    @Bean
    public Queue etlFailedQueue() {
        return QueueBuilder.durable(ETL_FAILED_QUEUE).build();
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(etlQueue()).to(etlExchange()).with(ETL_ROUTING_KEY);
    }
}
