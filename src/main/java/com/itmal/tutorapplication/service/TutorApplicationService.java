package com.itmal.tutorapplication.service;

import com.itmal.tutorapplication.domain.TutorApplication;

import java.util.List;

public interface TutorApplicationService {
    void apply(Long userId);
    void approve(Long applicationId);
    void reject(Long applicationId);
    List<TutorApplication> getPendingApplications();
}
