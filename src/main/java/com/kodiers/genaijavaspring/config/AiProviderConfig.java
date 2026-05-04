package com.kodiers.genaijavaspring.config;

import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.huggingface.HuggingfaceChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderConfig {

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
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
}
