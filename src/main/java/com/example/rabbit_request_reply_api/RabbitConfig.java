package com.example.rabbit_request_reply_api;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String REQUEST_QUEUE = "request.queue";
    public static final String REPLY_QUEUE = "reply.queue";

    @Bean
    public Queue requestQueue() {
        return new Queue(REQUEST_QUEUE, false);
    }

    @Bean
    public Queue replyQueue() {
        return new Queue(REPLY_QUEUE, false);
    }
}

