package com.rag.spring_ai_chatbot.etl;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

import static com.rag.spring_ai_chatbot.config.RabbitConfig.*;

@Service
@RequiredArgsConstructor
public class EtlMessagePublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishFile(Path p) {
        rabbitTemplate.convertAndSend(ETL_EXCHANGE, ETL_ROUTING_KEY, p.toString());
    }
}
