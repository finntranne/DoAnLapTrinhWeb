package com.alotra.repository.common;

import com.alotra.entity.common.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {
    
    // Tìm device token của user
    List<DeviceToken> findByUser_UserIDAndIsActiveTrue(Integer userId);
    
    // Tìm theo device token
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    
    // Deactivate token
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.deviceToken = :token")
    void deactivateToken(@Param("token") String token);
    
    // Deactivate all tokens của user
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.user.id = :userId")
    void deactivateAllUserTokens(@Param("userId") Integer userId);
    
    // Xóa tokens không active
    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.isActive = false")
    void deleteInactiveTokens();
}