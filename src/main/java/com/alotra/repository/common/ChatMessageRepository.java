package com.alotra.repository.common;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.entity.common.MessageEntity;


@Repository
public interface ChatMessageRepository extends JpaRepository<MessageEntity, Long> {

	@Query("SELECT m FROM MessageEntity m " +
	           "WHERE m.customer.id = :customerId AND m.shop.shopId = :shopId " +
	           "ORDER BY m.timestamp ASC")
    List<MessageEntity> getChatHistory(@Param("customerId") Integer customerId,
                                       @Param("shopId") Integer shopId);


    List<MessageEntity> findByCustomer_IdOrderByTimestampAsc(Integer customerId);


    List<MessageEntity> findByShop_ShopIdOrderByTimestampAsc(Integer shopId);
    
    @Query("""
            SELECT m
            FROM MessageEntity m
            WHERE m.shop.shopId = :shopId
              AND m.senderType = 'CUSTOMER'
              AND m.isRead = false
            ORDER BY m.timestamp DESC
        """)
        List<MessageEntity> findUnreadMessagesByShop(@Param("shopId") Integer shopId, Pageable pageable);



    @Query("""
            SELECT COUNT(m)
            FROM MessageEntity m
            WHERE m.shop.shopId = :shopId
              AND m.senderType = 'CUSTOMER'
              AND m.isRead = false
        """)
        int countUnreadMessagesByShopId(@Param("shopId") Integer shopId);

        // 🔵 Đếm số tin chưa đọc GỬI TỪ SHOP → CUSTOMER
        @Query("""
            SELECT COUNT(m)
            FROM MessageEntity m
            WHERE m.customer.id = :customerId
              AND m.senderType = 'SHOP'
              AND m.isRead = false
        """)
        int countUnreadMessagesByCustomerId(@Param("customerId") Integer customerId);

      
        @Modifying
        @Transactional
        @Query("""
            UPDATE MessageEntity m
            SET m.isRead = true
            WHERE m.shop.shopId = :shopId
              AND m.customer.id = :customerId
              AND m.senderType = 'CUSTOMER'
              AND m.isRead = false
        """)
        int markCustomerMessagesAsRead(@Param("shopId") Integer shopId,
                                       @Param("customerId") Integer customerId);

      
        @Modifying
        @Transactional
        @Query("""
            UPDATE MessageEntity m
            SET m.isRead = true
            WHERE m.customer.id = :customerId
              AND m.shop.shopId = :shopId
              AND m.senderType = 'SHOP'
              AND m.isRead = false
        """)
        void markShopMessagesAsRead(@Param("customerId") Integer customerId,
                                    @Param("shopId") Integer shopId);

}