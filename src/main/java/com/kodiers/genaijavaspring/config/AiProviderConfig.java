package com.kodiers.genaijavaspring.config;

import com.kodiers.genaijavaspring.chat.advisor.ErrorWrappingAdvisor;
import com.kodiers.genaijavaspring.chat.advisor.SystemPromptAdvisor;
import com.kodiers.genaijavaspring.chat.advisor.ValidationAdvisor;
import com.kodiers.genaijavaspring.chat.openai.jailbreak.BankingTools;
import com.kodiers.genaijavaspring.rag.config.data.PgVectorStoreConfigData;
import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Configuration
public class AiProviderConfig {

    private static final int DEFAULT_TOP_K = 10;
    private static final int MAX_MESSAGES = 5;

    @Value("classpath:templates/vector-store-memory-system-prompt.st")
    private Resource vectorStoreMemorySystemPrompt;

    @Value("classpath:templates/prompt-store-memory-system-prompt.st")
    private Resource promptStoreMemorySystemPrompt;

    @Bean("chatMemoryVectorStore")
    public VectorStore chatMemoryVectorStore(JdbcTemplate jdbcTemplate,
                                             PgVectorStoreConfigData pgVectorStoreConfigData,
                                             @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForChatMemory())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Bean("ragVectorStore")
    public VectorStore ragVectorStore(JdbcTemplate jdbcTemplate, PgVectorStoreConfigData pgVectorStoreConfigData,
                                      @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(pgVectorStoreConfigData.getTableNameForRag())
                .initializeSchema(pgVectorStoreConfigData.isInitializeSchema())
                .dimensions(pgVectorStoreConfigData.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.valueOf(pgVectorStoreConfigData.getDistanceType()))
                .indexType(PgVectorStore.PgIndexType.valueOf(pgVectorStoreConfigData.getIndexType()))
                .maxDocumentBatchSize(pgVectorStoreConfigData.getMaxDocumentBatchSize())
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor ragAdvisor(@Qualifier("ragVectorStore") VectorStore vectorStore,
                                                   RagConfigData ragConfigData) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(ragConfigData.getTopK())
                        .similarityThreshold(ragConfigData.getSimilarityThreshold())
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false)
                        .build())
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter(RagConfigData ragConfigData) {
        return TokenTextSplitter.builder()
                .withChunkSize(ragConfigData.getChunk().getSize())
                .withMinChunkSizeChars(ragConfigData.getChunk().getMinChunkSize())
                .withMinChunkLengthToEmbed(ragConfigData.getChunk().getMinChunkToEmbed())
                .withMaxNumChunks(ragConfigData.getChunk().getMaxNumChunks())
                .withKeepSeparator(ragConfigData.getChunk().isKeepSeparator())
                .build();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(MAX_MESSAGES)
                .build();
    }

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel, SimpleLoggerAdvisor simpleLoggerAdvisor,
                                       SafeGuardAdvisor safeGuardAdvisor, ErrorWrappingAdvisor errorWrappingAdvisor,
                                       SystemPromptAdvisor systemPromptAdvisor, ValidationAdvisor validationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(safeGuardAdvisor, simpleLoggerAdvisor, errorWrappingAdvisor, systemPromptAdvisor, validationAdvisor)
                .build();
    }

    @Bean("openAiGeneralChatClient")
    public ChatClient openAiGeneralChatClient(OpenAiChatModel openAiChatModel, SafeGuardAdvisor safeGuardAdvisor) {
        return ChatClient.builder(openAiChatModel)
//                .defaultAdvisors(safeGuardAdvisor)
//                .defaultTools(bankingTools)
                .build();
    }

    @Bean("openAiChatClientWithMemory")
    public ChatClient openAiChatClientWithMemory(OpenAiChatModel openAiChatModel, ChatMemory chatMemory,
                                                 @Qualifier("chatMemoryVectorStore") VectorStore pgVectorStore) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(PromptChatMemoryAdvisor.builder(chatMemory)
                        .systemPromptTemplate(new PromptTemplate(promptStoreMemorySystemPrompt))
                        .build())
//                .defaultAdvisors(VectorStoreChatMemoryAdvisor.builder(pgVectorStore)
//                        .systemPromptTemplate(new PromptTemplate(vectorStoreMemorySystemPrompt))
//                        .defaultTopK(DEFAULT_TOP_K)
//                        .build(),
//                        MessageChatMemoryAdvisor.builder(chatMemory).build())
//                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean("vertexChatClient")
    public ChatClient vertexChatClient(VertexAiGeminiChatModel vertexAiGeminiChatModel) {
        return ChatClient.builder(vertexAiGeminiChatModel)
                .build();
    }

//    @Bean("huggingFaceChatClient")
//    public ChatClient huggingFaceChatClient(HuggingfaceChatModel huggingfaceChatModel) {
//        return ChatClient.builder(huggingfaceChatModel)
//                .build();
//    }

    @Bean("ollamaChatClient")
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .build();
    }

    @Bean("openAiRagChatClient")
    public ChatClient openAiRagChatClient(OpenAiChatModel openAiChatModel,
                                          SimpleVectorStore simpleVectorStore,
                                          SimpleLoggerAdvisor simpleLoggerAdvisor,
                                          RagConfigData ragConfigData) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(simpleVectorStore)
                        .searchRequest(SearchRequest.builder()
                                .topK(ragConfigData.getTopK())
                                .similarityThreshold(ragConfigData.getSimilarityThreshold())
                                .build())
                        .build(), simpleLoggerAdvisor)
                .build();
    }

    @Bean("openAiAdvancedRagChatClient")
    public ChatClient openAiAdvancedRagChatClient(OpenAiChatModel openAiChatModel,
                                                  RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build();
    }

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return embeddingModel;
    }

    @Bean
    public SimpleLoggerAdvisor simpleLoggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    @Bean
    public SafeGuardAdvisor safeGuardAdvisor() {
        return new SafeGuardAdvisor(List.of("password", "ssn", "credit card", "cvv", "cvc", "pin", "token",
                "private_key", "confidential", "secret", "internal only", "system prompt", "api key", "hack"));
    }
}
