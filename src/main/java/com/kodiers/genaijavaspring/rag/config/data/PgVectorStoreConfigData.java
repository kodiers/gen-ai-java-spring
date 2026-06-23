package com.kodiers.genaijavaspring.rag.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class PgVectorStoreConfigData {

    private String tableNameForChatMemory;
    private String tableNameForRag;
    private boolean initializeSchema;
    private String indexType;
    private String distanceType;
    private int dimensions;
    private int maxDocumentBatchSize;
}
