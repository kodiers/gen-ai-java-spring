package com.kodiers.genaijavaspring.chat.openai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/openai/chat")
public class OpenAiChatController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that summarizes any given content. "
            + "Ensure that the summary is concise, informative, and captures the key points. "
            + "Use a friendly and approachable tone while maintaining a professionalism."
            + "Do not answer anything other than the summarization. If the question is not about summarization, "
            + "respond with 'I am sorry, I cannot help with that.'";

    private final ChatClient chatClient;

    public OpenAiChatController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/summarize")
    public String summarize(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}
