package com.alotra.service.approval_request.request;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alotra.dto.product.ProductRequestDTO;
import com.alotra.entity.product.Topping;

import lombok.RequiredArgsConstructor;

@Service
public class VendorApprovalRequestService {

    private ApprovalCommand approvalCommand;
    
    @Autowired
    private CreateProductRequestCommand createProductRequestCommand;
    
    @Autowired
    private UpdateProductRequestCommand updateProductRequestCommand;
    
    @Autowired
    private DeleteProductRequestCommand deleteProductRequestCommand;
    
    public void setCommand(ApprovalCommand command) {
        this.approvalCommand = command;
    }

    public void submitCreateProductRequest(ProductRequestDTO request, Integer userId) {
    	createProductRequestCommand.execute(request, userId);
    }
    
    public void submitUpdateProductRequest(ProductRequestDTO request, Integer userId) {
    	updateProductRequestCommand.execute(request, userId);
    }
    
    public void submitDeleteProductRequest(ProductRequestDTO request, Integer userId) {
    	deleteProductRequestCommand.execute(request, userId);
    }
}
