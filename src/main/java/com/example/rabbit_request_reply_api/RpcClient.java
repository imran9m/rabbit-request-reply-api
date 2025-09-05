package com.example.rabbit_request_reply_api;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class RpcClient {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public String sendAndReceive(String payload) throws Exception {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        MessageProperties props = new MessageProperties();
        props.setReplyTo(RabbitConfig.REPLY_QUEUE);
        props.setCorrelationId(correlationId);
        Message message = new Message(payload.getBytes(), props);

        rabbitTemplate.send(RabbitConfig.REQUEST_QUEUE, message);

        return future.get(10, TimeUnit.SECONDS);
    }

    public void handleReply(Message message) {
        String correlationId = message.getMessageProperties().getCorrelationId();
        String body = new String(message.getBody());
        CompletableFuture<String> future = pendingRequests.remove(correlationId);
        if (future != null) {
            future.complete(body);
        }
    }
}


