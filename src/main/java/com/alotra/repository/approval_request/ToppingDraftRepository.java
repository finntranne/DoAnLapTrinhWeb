package com.alotra.repository.approval_request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alotra.entity.draft.ToppingDraft;

@Repository
public interface ToppingDraftRepository extends JpaRepository<ToppingDraft, Integer>{

}
