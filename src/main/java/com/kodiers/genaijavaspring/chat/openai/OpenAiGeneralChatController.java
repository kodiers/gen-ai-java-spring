package com.kodiers.genaijavaspring.chat.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAiGeneralChatController {

    private final ChatClient chatClient;

    public OpenAiGeneralChatController(@Qualifier("openAiGeneralChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/general-chat")
    public String generalChat(@RequestBody String message) {
//        ChatOptions chatOptions = ChatOptions.builder()
//                .maxTokens(1000)
////                .temperature(2.0)
////                .topP(0.9)
//                .stopSequences(List.of("END_OF_PARAGRAPH"))
//                .build();
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
