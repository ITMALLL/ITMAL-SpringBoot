package com.itmal.auth.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LearningLanguageMapper {

    Long findLanguageIdByName(String languageName);

    void insertUserLearningLanguage(@Param("userId") Long userId, @Param("languageId") Long languageId);
}
