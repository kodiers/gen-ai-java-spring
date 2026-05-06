package com.kodiers.genaijavaspring.chat.ollama;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/ollama/chat")
@RestController
public class OllamaChatController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that drafts professional and concise emails based on the user input. "
            + "Ensure that the emails are clear, polite, and tailored to the specified content. "
            + "Use a formal and respectful tone while maintaining a brevity.";

    private final ChatClient chatClient;

    public OllamaChatController(@Qualifier("ollamaChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/draft-email")
    public ChatClientResponse draftEmail(@RequestBody String message) {
        return chatClient.prompt()
//                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }
}
