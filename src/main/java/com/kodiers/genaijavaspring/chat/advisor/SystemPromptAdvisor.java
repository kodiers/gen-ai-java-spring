package com.kodiers.genaijavaspring.chat.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SystemPromptAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        chatClientRequest = updateSystemMessage(chatClientRequest);
        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        chatClientRequest = updateSystemMessage(chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    @Override
    public String getName() {
        return "SystemPromptAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private ChatClientRequest updateSystemMessage(ChatClientRequest chatClientRequest) {
        List<Message> messages = new ArrayList<>(chatClientRequest.prompt().getUserMessages());
        SystemMessage existingSystemMessage = chatClientRequest.prompt().getSystemMessage();
        messages.add(new SystemMessage("Do not answer anything other than the summarization. "
                + "If the question is not about summarization, "
                + "respond with 'I am sorry, I cannot help with that.'" + existingSystemMessage.getText()));
        chatClientRequest.mutate().prompt(Prompt.builder()
                        .messages(messages)
                        .build())
                .build();
        String systemMessage = chatClientRequest.prompt().getSystemMessage().getText();
        log.info("System message updated: {}", systemMessage);
        return chatClientRequest;
    }
}
