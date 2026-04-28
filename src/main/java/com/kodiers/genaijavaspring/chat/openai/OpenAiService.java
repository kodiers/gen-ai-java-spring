package com.kodiers.genaijavaspring.chat.openai;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiService {

    private static final String OPEN_AI_API_KEY = "OPENAI_API_KEY";
    private static final String OPEN_AI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPEN_AI_MODEL = "gpt-4o";
    private static final String CONTENT_TYPE = "application/json";
    private static final String SYSTEM_PROMPT = "You are a helpful assistant that summarizes any given content. "
            + "Ensure that the summary is concise, informative, and captures the key points. "
            + "Use a friendly and approachable tone while maintaining a professionalism."
            + "Do not answer anything other than the summarization. If the question is not about summarization, "
            + "respond with 'I am sorry, I cannot help with that.'";

    private final ObjectMapper objectMapper;

    public OpenAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String chat(String message) throws OpenAiChatException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = getRequest(message);
            var response = httpClient.execute(request, resp -> EntityUtils.toString(resp.getEntity()));
            return parseResponse(response);
        } catch (IOException e) {
            throw new OpenAiChatException("Could not call OpenAI API", e);
        }
    }

    private HttpPost getRequest(String message) {
        var request = new HttpPost(OPEN_AI_API_URL);
        var openAiApiKey = System.getenv(OPEN_AI_API_KEY);
        request.addHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey);
        Map<String, Object> userMessage = Map.of("role", "user", "content", message);
        Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
        Map<String, Object> body = Map.of("model", OPEN_AI_MODEL, "messages", List.of(systemMessage, userMessage));
        String requestBody = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(requestBody));
        return request;
    }

    private String parseResponse(String response) throws JacksonException {
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        return responseMap.get("choices").toString();
    }
}
