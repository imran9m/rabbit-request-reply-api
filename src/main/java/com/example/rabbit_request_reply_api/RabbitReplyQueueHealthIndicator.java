package com.example.rabbit_request_reply_api;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitReplyQueueHealthIndicator implements HealthIndicator {

    private final RabbitAdmin rabbitAdmin;

    public RabbitReplyQueueHealthIndicator(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    public Health health() {
        try {
            var queueInfo = rabbitAdmin.getRabbitTemplate()
                    .execute(channel -> channel.queueDeclarePassive(RabbitConfig.getReplyQueueName()));

            if (queueInfo != null) {
                return Health.up()
                        .withDetail("replyQueue", RabbitConfig.getReplyQueueName())
                        .withDetail("messageCount", queueInfo.getMessageCount())
                        .withDetail("consumerCount", queueInfo.getConsumerCount())
                        .build();
            } else {
                // Try to redeclare queue if missing
                RabbitConfig.redeclareReplyQueue(rabbitAdmin);
                return Health.up()
                        .withDetail("replyQueue", RabbitConfig.getReplyQueueName())
                        .withDetail("action", "Queue was missing, re-declared")
                        .build();
            }
        } catch (Exception e) {
            try {
                // Fallback: attempt redeclare on exception
                RabbitConfig.redeclareReplyQueue(rabbitAdmin);
                return Health.up()
                        .withDetail("replyQueue", RabbitConfig.getReplyQueueName())
                        .withDetail("action", "Re-declared after exception: " + e.getMessage())
                        .build();
            } catch (Exception ex) {
                return Health.down(ex)
                        .withDetail("replyQueue", RabbitConfig.getReplyQueueName())
                        .build();
            }
        }
    }
}
