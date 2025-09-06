package com.example.rabbit_request_reply_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 15;
    
    private final RabbitTemplate rabbitTemplate;
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public RpcClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String sendAndReceive(String payload) throws TimeoutException, InterruptedException, AmqpException {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        try {
            MessageProperties props = new MessageProperties();
            props.setReplyTo(RabbitConfig.getReplyQueueName());
            props.setCorrelationId(correlationId);
            Message message = new Message(payload.getBytes(), props);

            logger.debug("Sending message with correlation ID: {}", correlationId);
            rabbitTemplate.send(RabbitConfig.REQUEST_QUEUE, message);

            try {
                return future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                logger.warn("Request with correlation ID {} timed out after {} seconds", 
                           correlationId, DEFAULT_TIMEOUT_SECONDS);
                throw e;
            } catch (ExecutionException e) {
                logger.error("Error executing request with correlation ID: {}", correlationId, e);
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException("Request execution failed", cause);
            }
        } catch (AmqpException e) {
            logger.error("Failed to send message with correlation ID: {}", correlationId, e);
            throw e;
        } finally {
            // Always cleanup the pending request to prevent memory leaks
            CompletableFuture<String> removed = pendingRequests.remove(correlationId);
            if (removed != null && !removed.isDone()) {
                removed.cancel(true);
            }
        }
    }

    public void handleReply(Message message) {
        try {
            String correlationId = message.getMessageProperties().getCorrelationId();
            if (correlationId == null) {
                logger.warn("Received reply message without correlation ID");
                return;
            }
            
            String body = new String(message.getBody());
            logger.debug("Received reply for correlation ID: {}", correlationId);

            CompletableFuture<String> future = pendingRequests.remove(correlationId);
            if (future != null) {
                future.complete(body);
                logger.debug("Completed future for correlation ID: {}", correlationId);
            } else {
                logger.warn("Received reply for unknown correlation ID: {} (possibly timed out)", correlationId);
            }
        } catch (Exception e) {
            logger.error("Error handling reply message", e);
        }
    }
    
    /**
     * Gets the current number of pending requests (useful for monitoring)
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }
}




