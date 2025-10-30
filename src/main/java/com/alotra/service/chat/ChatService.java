package com.alotra.service.chat;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.alotra.entity.common.MessageEntity;
import com.alotra.repository.common.ChatMessageRepository;

@Service
public class ChatService {

    private final ChatMessageRepository messageRepository;

    public ChatService(ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public List<MessageEntity> getChatHistory(Integer customerId, Integer shopId) {
        return messageRepository.getChatHistory(customerId, shopId);
    }
    
    public List<MessageEntity> findRecentMessagesForShop(Integer shopId, int limit) {
        return messageRepository.findUnreadMessagesByShop(shopId, PageRequest.of(0, limit));
    }

    public int countUnreadMessages(Integer shopId) {
        return messageRepository.countUnreadMessagesByShopId(shopId);
    }

}
