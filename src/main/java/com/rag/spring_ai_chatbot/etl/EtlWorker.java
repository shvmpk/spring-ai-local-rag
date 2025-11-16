package com.rag.spring_ai_chatbot.etl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.FileSystemResource;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;

import static com.rag.spring_ai_chatbot.config.RabbitConfig.*;

@Service
@Slf4j
public class EtlWorker {

    private final ChromaVectorStore vectorStore;
    private final RabbitTemplate rabbitTemplate;
    private final int batchSize;

    public EtlWorker(ChromaVectorStore vectorStore, RabbitTemplate rabbitTemplate,
                     @Value("${etl.file.batch-size:64}") int batchSize) {
        this.vectorStore = vectorStore;
        this.rabbitTemplate = rabbitTemplate;
        this.batchSize = batchSize;
    }

    @RabbitListener(queues = ETL_QUEUE, concurrency = "4")
    public void onMessage(String filePath) {
        try {
            log.info("ETL Worker processing {}", filePath);
            var reader = new TikaDocumentReader(new FileSystemResource(filePath));
            var docs = reader.read();
            var splitter = new TokenTextSplitter(500,100,20,100,true);
            var chunks = splitter.apply(docs);

            // Accept in batches to limit memory pressure
            for (int i = 0; i < chunks.size(); i += batchSize) {
                int to = Math.min(i + batchSize, chunks.size());
                var sub = chunks.subList(i, to);
                vectorStore.accept(sub);
            }

            log.info("ETL indexed {}", filePath);
        } catch (Exception e) {
            log.error("ETL failed for {}: {}", filePath, e.getMessage(), e);
            // send to dead-letter queue for inspection
            try {
                rabbitTemplate.convertAndSend(ETL_FAILED_QUEUE, filePath);
            } catch (Exception ex) {
                log.error("Failed to send to DLQ: {}", ex.getMessage(), ex);
            }
        }
    }
}