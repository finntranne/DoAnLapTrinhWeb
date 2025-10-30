package com.alotra.dto.chat;

public class MessageResponse {
    private String content;

    // Cần constructor, getter
    public MessageResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}