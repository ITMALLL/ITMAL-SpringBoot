package com.itmal.comment.service;

import com.itmal.comment.domain.Comment;
import com.itmal.comment.dto.CommentRequestDto;
import com.itmal.comment.dto.CommentResponseDto;
import com.itmal.comment.mapper.CommentMapper;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;

    public CommentResponseDto createComment(Long answerId,
                                            Long userId,
                                            CommentRequestDto requestDto) {

        Comment comment = new Comment();
        comment.setAnswerId(answerId);
        comment.setUserId(userId);
        comment.setContent(requestDto.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        //DB에 저장
        commentMapper.insert(comment);
        //값을 다시 가져오기
        Comment saved = commentMapper.findById(comment.getCommentId());

        //화면에 보여줄 형태(CommentResponseDto)로 변환해서 반환
        CommentResponseDto responseDto = new CommentResponseDto();
        responseDto.setCommentId(saved.getCommentId());
        responseDto.setContent(saved.getContent());
        responseDto.setCreatedAt(saved.getCreatedAt());
        responseDto.setNickname(saved.getNickname());
        return responseDto;
    }


    public List<CommentResponseDto> getComments(Long answerId) {
        List<Comment> comments = commentMapper.findByAnswerId(answerId);
        List<CommentResponseDto> commentResponseDtos =  new ArrayList<>();  //빈 List<CommentResponseDto> 결과 리스트 하나 준비

        //for문으로 값을 담아 보내기
        for(Comment comment:comments){
            CommentResponseDto responseDto  = new CommentResponseDto();
            responseDto.setCommentId(comment.getCommentId());
            responseDto.setContent(comment.getContent());
            responseDto.setNickname(comment.getNickname());
            responseDto.setCreatedAt(comment.getCreatedAt());

            if (comment.getUpdatedAt() != null && !comment.getUpdatedAt().equals(comment.getCreatedAt())) {
                responseDto.setUpdatedAt(comment.getUpdatedAt());
            }
            commentResponseDtos.add(responseDto);

        }

        return commentResponseDtos;
    }


    public CommentResponseDto updateComment(Long commentId,
                                            Long userId,
                                            CommentRequestDto requestDto) {
        Comment comment = commentMapper.findById(commentId);    //댓글 존재 여부 확인
        if(comment == null){
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        } else if(!comment.getUserId().equals(userId)){
            throw new ApiException(ErrorCode.COMMENT_FORBIDDEN);
        } else{
            comment.setContent(requestDto.getContent());
            comment.setUpdatedAt(LocalDateTime.now());
        }

        commentMapper.update(comment);
        CommentResponseDto responseDto = new CommentResponseDto();
        responseDto.setCommentId(comment.getCommentId());
        responseDto.setContent(comment.getContent());
        responseDto.setNickname(comment.getNickname());
        responseDto.setCreatedAt(comment.getCreatedAt());
        responseDto.setUpdatedAt(comment.getUpdatedAt());

        //다시 조회해서 updateAt에 값 넣기
        return responseDto;

    }

    public int deleteComment(Long commentId,
                             Long userId) {
        Comment comment = commentMapper.findById(commentId);    //댓글 존재 여부 확인
        if(comment == null){
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        } else if(!comment.getUserId().equals(userId)){
            throw new ApiException(ErrorCode.COMMENT_FORBIDDEN);
        }
        return commentMapper.delete(commentId);

    }

    //마이페이지 댓글 조회용
    public List<CommentResponseDto> getMypageComment(Long userId){
        List<Comment> comments = commentMapper.findByUserId(userId);
        List<CommentResponseDto> commentResponseDtos = new ArrayList<>();

        for(Comment comment : comments){
            CommentResponseDto responseDto = new CommentResponseDto();
            responseDto.setCommentId(comment.getCommentId());
            responseDto.setContent(comment.getContent());
            responseDto.setCreatedAt(comment.getCreatedAt());
            if (comment.getUpdatedAt() != null && !comment.getUpdatedAt().equals(comment.getCreatedAt())) {
                responseDto.setUpdatedAt(comment.getUpdatedAt());
            }
            commentResponseDtos.add(responseDto);
        }

        return commentResponseDtos;
    }
}
