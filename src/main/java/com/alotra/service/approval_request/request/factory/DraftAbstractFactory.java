package com.alotra.service.approval_request.request.factory;

import com.alotra.entity.draft.DraftEntity;

public interface DraftAbstractFactory<T extends DraftEntity> {
    T createDraft(Object requestDTO, Integer userId);
    void save(T draft);
}