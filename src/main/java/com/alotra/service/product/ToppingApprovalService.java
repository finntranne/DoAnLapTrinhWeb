package com.alotra.service.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.product.ToppingApproval;
import com.alotra.repository.product.ToppingApprovalRepository;

@Service
public class ToppingApprovalService {
	
	private final ToppingApprovalRepository approvalRepository;
	
	public ToppingApprovalService(ToppingApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }
	public Page<ToppingApproval> findByStatus(String status, Pageable pageable) {
		return approvalRepository.findByStatus(status, pageable);
	}
	
	public Optional<ToppingApproval> findById(Integer id){
    	return approvalRepository.findById(id);
	}
	
	public void approveToppingChange(Integer approvalId, Integer reviewedByUserId) {
		approvalRepository.approveToppingChange(approvalId, reviewedByUserId);
    }
	
	public void rejectToppingChange(Integer approvalId, Integer reviewedByUserId, String rejectionReason) {
		approvalRepository.rejectToppingChange(approvalId, reviewedByUserId, rejectionReason);
    }

}
