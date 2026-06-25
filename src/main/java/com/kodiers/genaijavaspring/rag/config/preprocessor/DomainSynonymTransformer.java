package com.kodiers.genaijavaspring.rag.config.preprocessor;

import com.kodiers.genaijavaspring.rag.config.data.RagConfigData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DomainSynonymTransformer implements QueryTransformer {

    private final Map<Pattern, String> replacements;

    public DomainSynonymTransformer(RagConfigData ragConfigData) {
        Map<String, String> synonymMap = ragConfigData.getSynonyms();
        Map<Pattern, String> replacements = new LinkedHashMap<>();
        if (synonymMap != null) {
            synonymMap.forEach((key, value) -> replacements.put(
                    Pattern.compile("\\b" + Pattern.quote(key) + "\\b", Pattern.CASE_INSENSITIVE), value));
        }
        this.replacements = replacements;
    }

    @Override
    public Query transform(Query query) {
        String queryText = query.text();
        if (queryText.isBlank()) {
            return query;
        }
        String cleanedText = getCleanedText(queryText);
        cleanedText = applyDomainSynonyms(cleanedText);
        log.info("Transformed query: {}", cleanedText);
        return Query.builder()
                .text(cleanedText)
                .build();
    }

    private String getCleanedText(String queryText) {
        return queryText.trim()
                .replaceAll("(?i)^(please|can you|could you)\\s+", "")
                .replaceAll("\\s+\\?$", "");
    }

    private String applyDomainSynonyms(String cleanedText) {
        for (var entry : replacements.entrySet()) {
            cleanedText = entry.getKey().matcher(cleanedText).replaceAll(entry.getValue());
        }
        return cleanedText;
    }

}
