package com.itmal.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmal.comment.dto.CommentRequestDto;
import com.itmal.comment.service.CommentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private String requestBody() throws Exception {
        CommentRequestDto dto = new CommentRequestDto();
        dto.setContent("테스트 댓글");
        return new ObjectMapper().writeValueAsString(dto);
    }

    @Test
    @DisplayName("비인증 사용자 댓글 작성 시 4xx 반환")
    void createComment_unauthenticated_returns4xx() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(post("/api/answers/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("비인증 사용자 댓글 수정 시 4xx 반환")
    void updateComment_unauthenticated_returns4xx() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(put("/api/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("비인증 사용자 댓글 삭제 시 4xx 반환")
    void deleteComment_unauthenticated_returns4xx() throws Exception {
        // Arrange & Act & Assert
        mockMvc.perform(delete("/api/comments/1"))
                .andExpect(status().is4xxClientError());
    }
}
