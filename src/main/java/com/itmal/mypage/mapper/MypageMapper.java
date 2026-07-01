package com.itmal.mypage.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MypageMapper {

    int countQuestionByUserId(Long userId);

    int countAnswerByUserId(Long userId);

    int countCommentByUserId(Long userId);

}
