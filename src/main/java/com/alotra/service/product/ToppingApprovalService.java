package com.alotra.service.product;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.alotra.entity.ApprovalRequest;
//import com.alotra.entity.product.ToppingApproval;
import com.alotra.repository.approval_request.ApprovalRequestRepository;
//import com.alotra.repository.product.ToppingApprovalRepository;

@Service
public class ToppingApprovalService {
	
	@Autowired
	private ApprovalRequestRepository approvalRequestRepository;
	
	public Page<ApprovalRequest> findAll(Pageable pageable) {
		return approvalRequestRepository.findAll(pageable);
	}
	
//	public Page<ToppingApproval> findByStatus(String status, Pageable pageable) {
//		return approvalRepository.findByStatus(status, pageable);
//	}
//	
//	public Optional<ToppingApproval> findById(Integer id){
//    	return approvalRepository.findById(id);
//	}
//	
//	public void approveToppingChange(Integer approvalId, Integer reviewedByUserId) {
//		approvalRepository.approveToppingChange(approvalId, reviewedByUserId);
//    }
//	
//	public void rejectToppingChange(Integer approvalId, Integer reviewedByUserId, String rejectionReason) {
//		approvalRepository.rejectToppingChange(approvalId, reviewedByUserId, rejectionReason);
//    }

}
