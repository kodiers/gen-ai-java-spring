package com.kodiers.genaijavaspring.chat.openai;

public class OpenAiChatException extends Exception {

    public OpenAiChatException() {
    }

    public OpenAiChatException(String message) {
        super(message);
    }

    public OpenAiChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
