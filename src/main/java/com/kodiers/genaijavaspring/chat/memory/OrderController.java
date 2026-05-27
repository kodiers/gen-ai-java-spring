package com.kodiers.genaijavaspring.chat.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders/chat")
public class OrderController {

    private final ChatClient chatClient;
    private final OrderStatusTools orderStatusTools;
    private final ChatMemory chatMemory;

    private final static String SYSTEM_PROMPT = "You are a helpful assistant that manages orders. "
            + "You can help with order status updates, tracking, and customer support. "
            + "Always provide clear and concise responses."
            + "If user provides the Order ID, you can find the order in the memory and update its status, use "
            + "available tool `getOrderStatus(orderId).";

    public OrderController(@Qualifier("openAiChatClientWithMemory") ChatClient chatClient,
                           OrderStatusTools orderStatusTools, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.orderStatusTools = orderStatusTools;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/status")
    public String orderStatus(@RequestBody String message, @RequestHeader(value = "user-id") String userId) {
        log.info("Chat memory for user : {}", userId);
        chatMemory.get(userId).forEach(m -> log.info("{}: {}", m.getMessageType(), m.getMetadata()));
        String userMessage = message + ", user id: " + userId;
        return chatClient.prompt()
                .tools(orderStatusTools)
                .system(SYSTEM_PROMPT)
                .user(u -> u.text("User query: {userMessage}").param("userMessage", userMessage))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                .call()
                .content();
    }
}
