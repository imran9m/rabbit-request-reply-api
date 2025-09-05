package com.example.rabbit_request_reply_api;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReplyListener {
    private final RpcClient rpcClient;
    public ReplyListener(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @RabbitListener(queues = RabbitConfig.REPLY_QUEUE)
    public void receiveReply(Message message) {
        rpcClient.handleReply(message);
    }
}
