package com.alotra.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alotra.dto.chat.ChatMessage;
import com.alotra.entity.common.MessageEntity;
import com.alotra.entity.shop.Shop;
import com.alotra.entity.user.User;
import com.alotra.repository.common.ChatMessageRepository;
import com.alotra.repository.shop.ShopRepository;
import com.alotra.repository.user.UserRepository;

@Controller
public class ChatController {
	
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository messageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
    }
    
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    ShopRepository shopRepository;

    @MessageMapping("/sendMessage")
    public void sendMessage(ChatMessage message) {

        Integer customerId = message.getFrom();
        Integer shopId = message.getTo();

        System.out.printf("message:", message);
        // 1. Lấy entity
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        // 2. Tạo MessageEntity
        MessageEntity entity = new MessageEntity();
        entity.setCustomer(customer);
        entity.setShop(shop);
        entity.setContent(message.getContent());
        entity.setTimestamp(LocalDateTime.now());
        entity.setSenderType("CUSTOMER"); // 👈 THÊM DÒNG NÀY

        // 3. Lưu DB
        messageRepository.save(entity);

        // 4. Gửi realtime tới shop
        messagingTemplate.convertAndSend("/topic/shop/" + shop.getShopId(), message);
    }
    
    @MessageMapping("/shopReply")
    public void shopReply(ChatMessage message) {
        Integer shopId = message.getFrom();
        Integer customerId = message.getTo();

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + shopId));
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        // 🔹 Lưu tin nhắn vào database
        MessageEntity entity = new MessageEntity();
        entity.setShop(shop);
        entity.setCustomer(customer);
        entity.setContent(message.getContent());
        entity.setTimestamp(LocalDateTime.now());
        entity.setSenderType("SHOP");
        entity.setRead(false); 

        messageRepository.save(entity);

        // 🔹 Chuẩn bị dữ liệu gửi về WebSocket
        ChatMessage response = new ChatMessage();
        response.setFrom(shopId);
        response.setTo(customerId);
        response.setContent(message.getContent());
        response.setType("SHOP");

        // 🔹 Gửi tin cho khách hàng
        messagingTemplate.convertAndSend("/topic/customer/" + customerId, response);
    }


    
    @GetMapping("/chat")
    public String chatPageCustomer(@RequestParam Integer customerId,
                           @RequestParam Integer shopId,
                           Model model) {

        List<MessageEntity> chatHistory = messageRepository.getChatHistory(customerId, shopId);

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("customerId", customerId);
        model.addAttribute("shopId", shopId);

        return "chat/chat"; // trả về chat.html
    }
    
    @GetMapping("/vendor/chat")
    public String chatPageForShop(@RequestParam Integer customerId,
    								@RequestParam Integer shopId,
                                  Model model) {

    	messageRepository.markCustomerMessagesAsRead(shopId, customerId);

        List<MessageEntity> chatHistory = messageRepository.getChatHistory(customerId, shopId);

        model.addAttribute("chatHistory", chatHistory);
        model.addAttribute("customerId", customerId);
        model.addAttribute("shopId", shopId);

        return "chat/chatfromVendor"; 
    }

   

}