package com.kodiers.genaijavaspring.chat.huggingface;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/huggingface/chat")
@RestController
public class HuggingFaceChatController {

    private static final String SYSTEM_PROMPT = "You are a senior engineer. Generate code based on the given description. "
            + "Ensure that the code is idiomatic, efficient, readable, and follows best practices. ";

    private final ChatClient chatClient;

    public HuggingFaceChatController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @RequestMapping("/generate-code")
    public ChatClientResponse generateCode(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .chatClientResponse();
    }

}
