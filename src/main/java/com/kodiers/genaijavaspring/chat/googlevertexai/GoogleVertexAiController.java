package com.kodiers.genaijavaspring.chat.googlevertexai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vertexai/chat")
public class GoogleVertexAiController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that generates professional Java comments for Java code. "
            + "Always explain the purpose of the class or method, its parameters, return values and any exceptions it may throw."
            + "Use standard Java doc style with /** ... */."
            + "Keep explanations concise, clear and technical.";

    private final ChatClient chatClient;

    public GoogleVertexAiController(@Qualifier("vertexChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/generate-java-docs")
    public ChatClientResponse generateJavaDocs(@RequestBody String message) {
        return chatClient.prompt()
                .user(message)
                .system(SYSTEM_PROMPT)
                .call()
                .chatClientResponse();
    }
}
