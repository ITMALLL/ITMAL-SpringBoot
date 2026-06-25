package com.itmal.comment.mapper;

import com.itmal.comment.domain.Comment;
import com.itmal.comment.dto.CommentResponseDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper {

    int insert(Comment comment);

    List<Comment> findByAnswerId(Long answerId);    //답변에서 댓글 보여주기

    Comment findById(Long commentId);   //댓글 존재 여부 확인 내부 메서드

    int update(Comment comment);

    int delete(Long commentId);

    //List<CommentResponseDto> findByUserId(Long userId);  //마이페이지 내 댓글 조회
}
