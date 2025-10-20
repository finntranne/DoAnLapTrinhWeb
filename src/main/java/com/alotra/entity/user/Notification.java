package com.alotra.entity.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NotificationID")
    private Integer notificationID;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private User user;
    
    @Column(name = "Title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "Message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;
    
    @Column(name = "Type", nullable = false, length = 50)
    private String type;
    
    @Column(name = "RelatedEntityID")
    private Integer relatedEntityID;
    
    @Column(name = "IsRead", nullable = false)
    private Boolean isRead = false;
    
    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}