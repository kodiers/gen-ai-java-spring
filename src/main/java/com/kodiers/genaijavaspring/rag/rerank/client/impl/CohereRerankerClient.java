package com.kodiers.genaijavaspring.rag.rerank.client.impl;

import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import com.kodiers.genaijavaspring.rag.rerank.client.RerankerClient;
import com.kodiers.genaijavaspring.rag.rerank.exception.RerankException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class CohereRerankerClient implements RerankerClient {

    private final RagConfigData.RerankProperties rerankProperties;
    private final ObjectMapper objectMapper;

    public CohereRerankerClient(RagConfigData ragConfigData, ObjectMapper objectMapper) {
        this.rerankProperties = ragConfigData.getRerank();
        this.objectMapper = objectMapper;
    }

    @Override
    public double[] score(String query, List<String> documents) throws RerankException {
        ObjectNode root = getJsonEntityFromRerank(query, documents);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            JsonNode node = executeRerankRequest(root, httpClient);
            double[] scores = getScores(documents, node);
            setScoresForUntouched(scores);
            return scores;
        } catch (IOException e) {
            throw new RerankException("Error while reranking", e);
        }
    }

    private ObjectNode getJsonEntityFromRerank(String query, List<String> documents) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", rerankProperties.getModel());
        root.put("query", query);
        ArrayNode docs = objectMapper.createArrayNode();
        for (String doc : documents) {
            docs.add(doc);
        }
        root.set("documents", docs);
        root.put("return_documents", false);
        return root;
    }

    private JsonNode executeRerankRequest(ObjectNode root, CloseableHttpClient httpClient) throws IOException {
        HttpPost httpPostRequest = new HttpPost(rerankProperties.getUrl());
        httpPostRequest.addHeader("Authorization", "Bearer " + rerankProperties.getApiKey());
        httpPostRequest.addHeader("Content-Type", "application/json");
        httpPostRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8));
        return httpClient.execute(httpPostRequest, response -> objectMapper
                .readTree(response.getEntity().getContent()));
    }

    private double[] getScores(List<String> documents, JsonNode node) {
        var rerankResults = node.path("results");
        double[] scores = new double[documents.size()];
        Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        for (var rerankResult : rerankResults) {
            int index = rerankResult.path("index").asInt();
            double score = rerankResult.path("relevance_score").asDouble();
            if (index >= 0 && index < scores.length) {
                scores[index] = score;
            }
        }
        return scores;
    }

    private void setScoresForUntouched(double[] scores) {
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] == Double.NEGATIVE_INFINITY) {
                scores[i] = -1.0;
            }
        }
    }
}
