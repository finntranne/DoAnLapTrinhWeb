package com.alotra.dto.chat;

public class PrivateMessageRequest {

	private String content;
    private String recipient; // Tên (username) của người nhận

    // Getters and Setters
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getRecipient() {
        return recipient;
    }
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
