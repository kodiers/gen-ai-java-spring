package com.kodiers.genaijavaspring.rag.rerank.client;

import com.kodiers.genaijavaspring.rag.rerank.exception.RerankException;

import java.util.List;

public interface RerankerClient {

    double[] score(String query, List<String> documents) throws RerankException;
}
