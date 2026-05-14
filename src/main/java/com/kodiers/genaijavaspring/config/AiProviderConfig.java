package com.kodiers.genaijavaspring.config;

import com.kodiers.genaijavaspring.chat.advisor.ErrorWrappingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiProviderConfig {

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel, SimpleLoggerAdvisor simpleLoggerAdvisor,
                                       SafeGuardAdvisor safeGuardAdvisor, ErrorWrappingAdvisor errorWrappingAdvisor) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(safeGuardAdvisor, simpleLoggerAdvisor, errorWrappingAdvisor)
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
