package com.alotra.service.product;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alotra.entity.product.ProductApproval;
import com.alotra.repository.product.ProductApprovalRepository;

@Service
public class ProductApprovalService {
	
	private static final String PENDING_STATUS = "Pending"; // Định nghĩa hằng số trạng thái

    private final ProductApprovalRepository approvalRepository;

    public ProductApprovalService(ProductApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    /**
     * Lấy danh sách các yêu cầu phê duyệt chờ xử lý cho một Cửa hàng cụ thể.
     * Sử dụng: findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc
     */
    public List<ProductApproval> getPendingApprovalsByShop(Integer shopId) {
        // Truy vấn dữ liệu chờ phê duyệt, sắp xếp theo thời gian yêu cầu mới nhất
        return approvalRepository.findByProduct_Shop_ShopIdAndStatusOrderByRequestedAtDesc(shopId, PENDING_STATUS);
    }
    
    /**
     * Lấy danh sách tất cả các yêu cầu phê duyệt chờ xử lý trong toàn hệ thống.
     * Sử dụng: findAllPendingApprovals (Query tùy chỉnh)
     */
    public List<ProductApproval> getAllPendingApprovals() {
        // Truy vấn tất cả dữ liệu chờ phê duyệt, sắp xếp theo thời gian yêu cầu cũ nhất (ASC)
        return approvalRepository.findAllPendingApprovals();
    }
    
    /**
     * Đếm tổng số yêu cầu chờ phê duyệt trong toàn hệ thống.
     * Sử dụng: countAllPending (Query tùy chỉnh)
     */
    public Long countTotalPendingApprovals() {
        return approvalRepository.countAllPending();
    }


	public Page<ProductApproval> findByStatus(String status, Pageable pageable) {
		return approvalRepository.findByStatus(status, pageable);
	}
	
	public Optional<ProductApproval> findById(Integer approvalId){
		return approvalRepository.findById(approvalId);
	}
	
	public void approveProductChange(Integer approvalId, Integer reviewedByUserId) {
		approvalRepository.approveProductChange(approvalId, reviewedByUserId);
    }
	
	public void rejectProductChange(Integer approvalId, Integer reviewedByUserId, String rejectionReason) {
		approvalRepository.rejectProductChange(approvalId, reviewedByUserId, rejectionReason);
    }

}
