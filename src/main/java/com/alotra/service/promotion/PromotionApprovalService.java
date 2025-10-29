package com.alotra.service.promotion;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.ProductApproval;
import com.alotra.entity.promotion.PromotionApproval;
import com.alotra.repository.promotion.PromotionApprovalRepository;
@Service
public class PromotionApprovalService {

	@Autowired
	PromotionApprovalRepository approvalRepository;
	public List<PromotionApproval> getAllPendingApprovals() {
        // Truy vấn tất cả dữ liệu chờ phê duyệt, sắp xếp theo thời gian yêu cầu cũ nhất (ASC)
        return approvalRepository.findAllPendingApprovals();
    }

    public Long countTotalPendingApprovals() {
        return approvalRepository.countAllPending();
    }


	public Page<PromotionApproval> findByStatus(String status, Pageable pageable) {
		return approvalRepository.findByStatus(status, pageable);
	}
	
	public Optional<PromotionApproval> findById(Integer approvalId){
		return approvalRepository.findById(approvalId);
	}
	
	public void approveProductChange(Integer approvalId, Integer reviewedByUserId) {
		approvalRepository.approveProductChange(approvalId, reviewedByUserId);
    }
	
	public void rejectProductChange(Integer approvalId, Integer reviewedByUserId, String rejectionReason) {
		approvalRepository.rejectProductChange(approvalId, reviewedByUserId, rejectionReason);
    }
}
