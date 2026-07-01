package com.itmal.tutorapplication.service;

import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.auth.repository.UserMapper;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.tutorapplication.domain.ApplicationStatus;
import com.itmal.tutorapplication.domain.TutorApplication;
import com.itmal.tutorapplication.dto.TutorApplicationResponse;
import com.itmal.tutorapplication.mapper.TutorApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorApplicationServiceImpl implements TutorApplicationService {

    private final TutorApplicationMapper tutorApplicationMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void apply(Long userId) {
        User user = userMapper.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == Role.ROLE_TUTOR) {
            throw new BusinessException(ErrorCode.ALREADY_TUTOR);
        }
        if (tutorApplicationMapper.findPendingByUserId(userId) != null) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        TutorApplication application = new TutorApplication();
        application.setUserId(userId);
        application.setStatus(ApplicationStatus.PENDING);
        tutorApplicationMapper.insert(application);
    }

    @Override
    @Transactional
    public void approve(Long applicationId) {
        TutorApplication application = tutorApplicationMapper.findById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.TUTOR_APPLICATION_NOT_FOUND);
        }

        application.setStatus(ApplicationStatus.APPROVED);
        tutorApplicationMapper.updateStatus(application);
        userMapper.updateRole(application.getUserId(), Role.ROLE_TUTOR.name());
    }

    @Override
    @Transactional
    public void reject(Long applicationId) {
        TutorApplication application = tutorApplicationMapper.findById(applicationId);
        if (application == null) {
            throw new BusinessException(ErrorCode.TUTOR_APPLICATION_NOT_FOUND);
        }

        application.setStatus(ApplicationStatus.REJECTED);
        tutorApplicationMapper.updateStatus(application);
    }

    @Override
    public List<TutorApplication> getPendingApplications() {
        return tutorApplicationMapper.findAllPending();
    }

    @Override
    public List<TutorApplicationResponse> getPendingApplicationsWithUser() {
        return tutorApplicationMapper.findAllPendingWithUser();
    }

    @Override
    public boolean hasPendingApplication(Long userId) {
        return tutorApplicationMapper.findPendingByUserId(userId) != null;
    }
}
