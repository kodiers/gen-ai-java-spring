package com.kodiers.genaijavaspring.chat.dockermodelrunner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/docker-model-runner/chat")
@RestController
public class DockerModelRunnerController {

    private static final String SYSTEM_PROMPT = "You are a helpful assistant that generates professional LinkedIn posts about technical subjects. "
            + "Ensure that the posts are engaging, informative, and tailored to the professional audience. "
            + "Use a friendly and approachable tone while maintaining a professionalism.";

    private final ChatClient chatClient;

    public DockerModelRunnerController(@Qualifier("openAiChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @PostMapping("/linkedin-post-generator")
    public String generateLinkedInPost(@RequestBody String message) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(message)
                .call()
                .content();
    }
}
