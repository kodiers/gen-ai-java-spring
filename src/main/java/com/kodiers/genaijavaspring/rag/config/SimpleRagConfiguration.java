package com.kodiers.genaijavaspring.rag.config;

import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class SimpleRagConfiguration {

    private static final String FILES_PATH = "classpath:rag/mini-employee-handbook/files/*.md";
    private static final String VECTOR_STORE_PATH = "src/main/resources/rag/mini-employee-handbook/vector-store/vectorstore.json";

    @Bean
    public SimpleVectorStore simpleVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
                                               TokenTextSplitter textSplitter,
                                               RagConfigData ragConfigData) throws IOException {
        var simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        var vectorStoreFile = new File(VECTOR_STORE_PATH);
        if (vectorStoreFile.exists() && !ragConfigData.isForceRebuild()) {
            log.info("Loading vector store from file: {}", VECTOR_STORE_PATH);
            simpleVectorStore.load(vectorStoreFile);
            return simpleVectorStore;
        }
        log.info("Building vector store from files: {}", FILES_PATH);
        var documents = loadDocuments();
        var chunks = getChunks(documents, textSplitter);
        simpleVectorStore.add(chunks);
        simpleVectorStore.save(vectorStoreFile);
        return simpleVectorStore;
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

    private List<Document> getChunks(List<Document> documents, TokenTextSplitter textSplitter) {
        return textSplitter.apply(documents);
    }

    private List<Document> loadDocuments() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(FILES_PATH);
        List<Document> documents = Arrays.stream(resources)
                .flatMap(resource -> {
                    var reader = new TextReader(resource);
                    reader.getCustomMetadata().put("category", resource.getFilename().replace(".md", ""));
                    reader.getCustomMetadata().put("access_level", "public");
                    reader.getCustomMetadata().put("version", "2026.01");
                    return reader.read().stream();
                }).toList();
        return documents;
    }
}
