package com.itmal.tutorapplication.service;

import com.itmal.tutorapplication.domain.TutorApplication;
import com.itmal.tutorapplication.dto.TutorApplicationResponse;

import java.util.List;

public interface TutorApplicationService {
    void apply(Long userId);
    void approve(Long applicationId);
    void reject(Long applicationId);
    List<TutorApplication> getPendingApplications();
    List<TutorApplicationResponse> getPendingApplicationsWithUser();
}
