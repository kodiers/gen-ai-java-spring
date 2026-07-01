package com.kodiers.genaijavaspring.rag.service;

import com.kodiers.genaijavaspring.rag.config.data.PgVectorStoreConfigData;
import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import com.kodiers.genaijavaspring.rag.config.data.RagConstants;
import com.kodiers.genaijavaspring.rag.exception.RagException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RagIngestionService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final RagConfigData ragConfigData;
    private final PdfDocumentReaderConfig pdfDocumentReaderConfig;
    private final TokenTextSplitter tokenTextSplitter;
    private final String ragVectorStoreTableName;

    public RagIngestionService(@Qualifier("ragVectorStore") VectorStore vectorStore, JdbcTemplate jdbcTemplate,
                               RagConfigData ragConfigData,
                               TokenTextSplitter tokenTextSplitter,
                               PgVectorStoreConfigData pgVectorStoreConfigData) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.ragConfigData = ragConfigData;
        RagConfigData.PdfProperties pdfProperties = ragConfigData.getPdf();
        this.pdfDocumentReaderConfig = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(pdfProperties.getPagesPerDocument())
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withLeftAlignment(pdfProperties.isLeftAlignment())
                        .withNumberOfTopTextLinesToDelete(pdfProperties.getNumberOfTopTextLinesToDelete())
                        .withNumberOfBottomTextLinesToDelete(pdfProperties.getNumberOfBottomTextLinesToDelete())
                        .build())
                .build();
        this.tokenTextSplitter = tokenTextSplitter;
        this.ragVectorStoreTableName = pgVectorStoreConfigData.getTableNameForRag();
    }

    public void initializePgVectorStore() throws RagException {
        if (skipIngest(jdbcTemplate)) {
            return;
        }
        var pdfResources = getResources();
        if (pdfResources == null) {
            return;
        }
        ingestDocumentChunksToVectorStore(pdfResources);
    }

    public void upsertOneByPath(Path path) {
        log.info("Upserting document at path: {}", path);
        FileSystemResource resource = new FileSystemResource(path.toFile());
        String source = resource.getFilename();
        String checksum = sha256Hex(resource);
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + ragVectorStoreTableName
                + " WHERE metadata->>'source'= ? AND metadata->>'checksum'= ?", Integer.class, source, checksum);
        if (count != null && count > 0) {
            log.info("Document already exists in vector store, skipping upsert, checksum: {}", checksum);
            return;
        }
        deleteBySource(source);
        ingestDocumentChunksToVectorStore(new Resource[]{resource});
    }

    public void deleteBySource(String source) {
        jdbcTemplate.update("DELETE FROM " + ragVectorStoreTableName + " WHERE metadata->>'source'= ?", source);
        log.info("Deleted rows from vector store for source: {}", source);
    }

    private void ingestDocumentChunksToVectorStore(Resource[] pdfResources) {
        var documents = getDocuments(pdfResources);
        var chunks = getChunks(documents);
        addChunkIndex(chunks);
        vectorStore.add(chunks);
        log.info("Ingested {} chunks to vector store", chunks.size());
    }

    private void addChunkIndex(List<Document> chunks) {
        Map<String, Integer> counters = new HashMap<>();
        chunks.forEach(chunk -> {
            var source = String.valueOf(chunk.getMetadata().getOrDefault(RagConstants.SOURCE, RagConstants.UNKNOWN));
            var index = counters.merge(source, 1, Integer::sum) - 1;
            chunk.getMetadata().put(RagConstants.CHUNK_INDEX, index);
        });
    }

    private List<Document> getChunks(List<Document> documents) {
        return tokenTextSplitter.apply(documents);
    }

    private List<Document> getDocuments(Resource[] pdfResources) {
        List<Document> documents = new ArrayList<>();
        for (Resource resource : pdfResources) {
            List<Document> parts = getDocumentParts(resource);
            addMetadata(resource, parts);
            documents.addAll(parts);
        }
        return documents;
    }

    private void addMetadata(Resource resource, List<Document> parts) {
        parts.forEach(part -> {
            part.getMetadata().putIfAbsent(RagConstants.SOURCE, resource.getFilename());
            part.getMetadata().putIfAbsent(RagConstants.DOC_TYPE, resource.getFilename().substring(0,
                    resource.getFilename().indexOf(".")));
            part.getMetadata().putIfAbsent(RagConstants.UPDATED_AT, ZonedDateTime.now().toLocalDate().toString());
            String checksum = sha256Hex(resource);
            part.getMetadata().putIfAbsent(RagConstants.CHECKSUM, checksum);
        });
    }

    private List<Document> getDocumentParts(Resource resource) {
        if (RagConstants.PARAGRAPH.equalsIgnoreCase(ragConfigData.getPdf().getMode())) {
            return new ParagraphPdfDocumentReader(resource, pdfDocumentReaderConfig).read();
        }
        return new PagePdfDocumentReader(resource, pdfDocumentReaderConfig).read();
    }

    private Resource[] getResources() {
        var resolver = new PathMatchingResourcePatternResolver();
        String pdfPath = ragConfigData.getPdf().getPath();
        try {
            Resource[] resources = resolver.getResources(pdfPath);
            if (resources.length == 0) {
                log.warn("No PDF resources found at path: {}", pdfPath);
                return null;
            }
            return resources;
        } catch (Exception e) {
            log.error("Error loading PDF resources from path: {}", pdfPath, e);
            throw new RagException("Error loading PDF resources from path: " + pdfPath, e);
        }
    }

    private boolean skipIngest(JdbcTemplate jdbcTemplate) {
        if (ragConfigData.isForceRebuild()) {
            log.info("Forcing rebuild of vector store, truncating table: {}", ragVectorStoreTableName);
            jdbcTemplate.update("TRUNCATE TABLE " + ragVectorStoreTableName);
        } else {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + ragVectorStoreTableName,
                    Integer.class);
            if (count != null && count > 0) {
                log.info("Vector store already exists, skipping ingestion, rows: {}", count);
                return true;
            }
        }
        return false;
    }

    private String sha256Hex(Resource source) {
        final MessageDigest digest = newMessageDigest("SHA-256");
        try (InputStream inputStream = source.getInputStream();
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            digestInputStream.transferTo(OutputStream.nullOutputStream());
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new RagException("Error calculating checksum for resource: " + source.getFilename(), e);
        }
    }

    private MessageDigest newMessageDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RagException("No jca provider", e);
        }
    }
}
