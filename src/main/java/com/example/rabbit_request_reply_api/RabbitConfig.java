package com.example.rabbit_request_reply_api;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Configuration
public class RabbitConfig {

    public static final String REQUEST_QUEUE = "request.queue";
    private static String replyQueueName;

    static {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            replyQueueName = "reply.queue." + host + "." + UUID.randomUUID();
        } catch (UnknownHostException e) {
            replyQueueName = "reply.queue.unknown." + UUID.randomUUID();
        }
    }

    @Bean
    public Queue requestQueue() {
        return new Queue(REQUEST_QUEUE, false);
    }

    @Bean
    public Queue replyQueue(RabbitAdmin rabbitAdmin) {
        Queue queue = new Queue(replyQueueName, false, true, true);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    public static String getReplyQueueName() {
        return replyQueueName;
    }

    /** Helper to redeclare reply queue if lost */
    public static void redeclareReplyQueue(RabbitAdmin rabbitAdmin) {
        Queue queue = new Queue(replyQueueName, false, true, true);
        rabbitAdmin.declareQueue(queue);
    }
}