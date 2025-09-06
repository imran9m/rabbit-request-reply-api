package com.example.rabbit_request_reply_api;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final RpcClient rpcClient;

    public MessageController(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @PostMapping("/process")
    public ResponseEntity<String> processRequest(@RequestBody String payload) {
        try {
            // Input validation
            if (payload == null || payload.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Payload cannot be empty");
            }
            
            String result = rpcClient.sendAndReceive(payload);
            return ResponseEntity.ok(result);
            
        } catch (TimeoutException e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Request timeout - no response received within the specified time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Request was interrupted");
        } catch (AmqpException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Message broker is unavailable: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Processing failed: " + e.getMessage());
        }
    }
}


