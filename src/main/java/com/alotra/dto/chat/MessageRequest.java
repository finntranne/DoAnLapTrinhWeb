package com.alotra.dto.chat;

public class MessageRequest {
    private String name;

    // Cần constructor không tham số, getters và setters
    public MessageRequest() {
    }

    public MessageRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
