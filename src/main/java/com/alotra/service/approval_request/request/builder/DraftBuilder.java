package com.alotra.service.approval_request.request.builder;

public interface DraftBuilder<T, R> {
    T build(R request, Integer userId);
}

// T Draft, R DTO
