package com.alotra.repository.user;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alotra.entity.user.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId);
    
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    List<Notification> findUnreadNotificationsByUserId(@Param("userId") Integer userId, Pageable pageable);


    @Query("SELECT count(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countByUser_IdAndIsReadFalse(@Param("userId") Integer userId);


}