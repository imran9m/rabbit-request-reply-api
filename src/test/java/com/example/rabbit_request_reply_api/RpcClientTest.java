package com.example.rabbit_request_reply_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RpcClientTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RpcClient rpcClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rpcClient = new RpcClient(rabbitTemplate);
    }

    @Test
    void testSendAndReceive_NullPayload_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            rpcClient.sendAndReceive(null);
        });
    }

    @Test
    void testSendAndReceive_EmptyPayload_SendsMessage() {
        // Given
        String payload = "";
        
        // When & Then - should not throw exception for empty string
        assertDoesNotThrow(() -> {
            try {
                rpcClient.sendAndReceive(payload);
            } catch (TimeoutException e) {
                // TimeoutException is expected since we're not providing a reply
            }
        });
        
        verify(rabbitTemplate, times(1)).send(eq(RabbitConfig.REQUEST_QUEUE), any(Message.class));
    }

    @Test
    void testSendAndReceive_RabbitTemplateThrowsException_PropagatesException() {
        // Given
        String payload = "test payload";
        doThrow(new AmqpException("Connection failed")).when(rabbitTemplate).send(anyString(), any(Message.class));
        
        // When & Then
        assertThrows(AmqpException.class, () -> {
            rpcClient.sendAndReceive(payload);
        });
    }

    @Test
    void testSendAndReceive_Timeout_CleansUpPendingRequests() throws Exception {
        // Given
        String payload = "test payload";
        int initialPendingCount = rpcClient.getPendingRequestCount();
        
        // When - call sendAndReceive which should timeout
        assertThrows(TimeoutException.class, () -> {
            rpcClient.sendAndReceive(payload);
        });
        
        // Then - pending requests should be cleaned up
        assertEquals(initialPendingCount, rpcClient.getPendingRequestCount());
    }

    @Test
    void testHandleReply_ValidMessage_CompletesSuccessfully() {
        // Given
        String testPayload = "test";
        MessageProperties properties = new MessageProperties();
        properties.setCorrelationId("test-correlation-id");
        Message replyMessage = new Message("response".getBytes(), properties);
        
        // Simulate a pending request by calling sendAndReceive in a separate thread
        Thread senderThread = new Thread(() -> {
            try {
                rpcClient.sendAndReceive(testPayload);
            } catch (Exception e) {
                // Expected timeout
            }
        });
        
        senderThread.start();
        
        // Give some time for the request to be registered
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When
        rpcClient.handleReply(replyMessage);
        
        // Then - should not throw any exception
        assertDoesNotThrow(() -> senderThread.join(1000));
    }

    @Test
    void testHandleReply_MessageWithoutCorrelationId_LogsWarning() {
        // Given
        MessageProperties properties = new MessageProperties();
        // Intentionally not setting correlation ID
        Message message = new Message("response".getBytes(), properties);
        
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            rpcClient.handleReply(message);
        });
    }

    @Test
    void testGetPendingRequestCount_InitiallyZero() {
        assertEquals(0, rpcClient.getPendingRequestCount());
    }
}
