package com.example.rabbit_request_reply_api.exception;

/**
 * Custom exception for message processing errors in the RPC client.
 */
public class MessageProcessingException extends Exception {
    
    public MessageProcessingException(String message) {
        super(message);
    }
    
    public MessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
