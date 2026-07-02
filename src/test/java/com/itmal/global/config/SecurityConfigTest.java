package com.itmal.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ===== 어드민 접근 제어 =====

    @Test
    @DisplayName("비인증 사용자 - 어드민 페이지 접근 차단 (401)")
    @WithAnonymousUser
    void admin_anonymous_blocked() throws Exception {
        mockMvc.perform(get("/admin/tutor-applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER - 어드민 페이지 접근 시 403")
    @WithMockUser(roles = "USER")
    void admin_user_forbidden() throws Exception {
        mockMvc.perform(get("/admin/tutor-applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN - 어드민 페이지 접근 허용")
    @WithMockUser(roles = "ADMIN")
    void admin_admin_allowed() throws Exception {
        mockMvc.perform(get("/admin/tutor-applications"))
                .andExpect(status().isOk());
    }

    // ===== 채팅 수락/거절 접근 제어 =====

    @Test
    @DisplayName("USER - 채팅 수락 시 403")
    @WithMockUser(roles = "USER")
    void chatAccept_user_forbidden() throws Exception {
        mockMvc.perform(put("/api/chat-request/1/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER - 채팅 거절 시 403")
    @WithMockUser(roles = "USER")
    void chatReject_user_forbidden() throws Exception {
        mockMvc.perform(put("/api/chat-request/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
