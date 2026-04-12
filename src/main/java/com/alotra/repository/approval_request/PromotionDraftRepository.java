package com.alotra.repository.approval_request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.draft.PromotionDraft;

@Repository
public interface PromotionDraftRepository extends JpaRepository<PromotionDraft, Integer>{

}
