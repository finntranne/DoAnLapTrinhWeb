package com.alotra.repository.approval_request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.draft.ProductVariantDraft;

@Repository
public interface ProductVariantDraftRepository extends JpaRepository<ProductVariantDraft, Integer>{

}
