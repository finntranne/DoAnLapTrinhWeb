package com.alotra.dto.chat;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessage {
    private Integer from;
    private Integer to; 
    private String content;
    private String type;
	
}

