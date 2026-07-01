package com.itmal.tutorapplication.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.auth.domain.Role;
import com.itmal.auth.domain.User;
import com.itmal.global.exception.BusinessException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.tutorapplication.service.TutorApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TutorApplicationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TutorApplicationService tutorApplicationService;

    @InjectMocks
    private TutorApplicationController tutorApplicationController;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tutorApplicationController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        User mockUser = User.builder()
                .email("test@itmal.com")
                .nickname("테스터")
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        userDetails = new CustomUserDetails(mockUser) {
            @Override
            public Long getUserId() { return 1L; }
        };

        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===== 튜터 신청 =====

    @Test
    @DisplayName("튜터 신청 성공 - 마이페이지로 리다이렉트")
    void apply_success() throws Exception {
        mockMvc.perform(post("/tutor-applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));

        verify(tutorApplicationService).apply(eq(1L));
    }

    @Test
    @DisplayName("이미 신청 중인 경우 - 마이페이지로 리다이렉트")
    void apply_alreadyApplied_redirectsWithError() throws Exception {
        doThrow(new BusinessException(ErrorCode.ALREADY_APPLIED))
                .when(tutorApplicationService).apply(1L);

        mockMvc.perform(post("/tutor-applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));
    }

    @Test
    @DisplayName("이미 튜터인 경우 - 마이페이지로 리다이렉트")
    void apply_alreadyTutor_redirectsWithError() throws Exception {
        doThrow(new BusinessException(ErrorCode.ALREADY_TUTOR))
                .when(tutorApplicationService).apply(1L);

        mockMvc.perform(post("/tutor-applications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));
    }

    // ===== 관리자 목록 조회 =====

    @Test
    @DisplayName("관리자 - 튜터 신청 목록 조회")
    void getPendingApplications_success() throws Exception {
        when(tutorApplicationService.getPendingApplicationsWithUser()).thenReturn(List.of());

        mockMvc.perform(get("/admin/tutor-applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tutor-applications"))
                .andExpect(model().attributeExists("applications"));
    }

    // ===== 관리자 승인 =====

    @Test
    @DisplayName("관리자 - 튜터 신청 승인 성공")
    void approve_success() throws Exception {
        mockMvc.perform(post("/admin/tutor-applications/1/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tutor-applications"));

        verify(tutorApplicationService).approve(eq(1L));
    }

    // ===== 관리자 거절 =====

    @Test
    @DisplayName("관리자 - 튜터 신청 거절 성공")
    void reject_success() throws Exception {
        mockMvc.perform(post("/admin/tutor-applications/1/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tutor-applications"));

        verify(tutorApplicationService).reject(eq(1L));
    }
}
