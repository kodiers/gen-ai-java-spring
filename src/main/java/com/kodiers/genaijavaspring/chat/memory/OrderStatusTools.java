package com.kodiers.genaijavaspring.chat.memory;

import com.kodiers.genaijavaspring.chat.memory.dto.Order;
import com.kodiers.genaijavaspring.chat.memory.dto.OrderStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

//import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderStatusTools {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (int i = 1; i <= 10; i++) {
            String orderId = "ORD-" + i;
            Order order = new Order(
                    orderId,
                    i % 2 == 0 ? "UPS" : "FedEx",
                    OrderStatus.CREATED,
                    "user-" + i,
                    "User " + i
            );
            orders.put(orderId, order);
        }
    }

    @Tool
    public String getOrderStatus(@ToolParam(description = "order ID") String orderId,
                                 @ToolParam(description = "user ID") String userId) {
        Order order = orders.get(orderId);
        if (order != null && !order.userId().equals(userId)) {
            return "Order ID: " + orderId + " not belong to user";
        }
        if (order == null) {
            return "Order not found";
        }
        OrderStatus currentStatus = order.status();
        OrderStatus nextStatus = nextStage(currentStatus);
        Order updatedOrder = new Order(
                order.orderId(),
                order.carrier(),
                nextStatus,
                order.userId(),
                order.userName()
        );
        orders.put(orderId, updatedOrder);
        return "Order " + orderId + " for " + order.userName() + " is now " + currentStatus;
    }

    private OrderStatus nextStage(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case CREATED -> OrderStatus.PROCESSING;
            case PROCESSING -> OrderStatus.SHIPPED;
            case SHIPPED, DELIVERED -> OrderStatus.DELIVERED;
        };
    }
}
