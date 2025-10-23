package com.alotra.dto.response;

import java.time.LocalDateTime;

import com.alotra.enums.ActionType;
import com.alotra.enums.ApprovalStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponseDTO {
    private Integer approvalId;
    private String entityType; // PRODUCT, PROMOTION
    private Integer entityId;
    private String actionType;
    private String status;
    private String changeDetails;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private String rejectionReason;
    private String requestedByName;
    private String reviewedByName;
    private String entityName;
}
