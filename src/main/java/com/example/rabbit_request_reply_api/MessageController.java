package com.example.rabbit_request_reply_api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final RpcClient rpcClient;

    public MessageController(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @PostMapping("/process")
    public String processRequest(@RequestBody String payload) throws Exception {
        return rpcClient.sendAndReceive(payload);
    }
}


