package com.itmal.auth.repository;

import com.itmal.question.dto.LanguageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LearningLanguageMapper {

    Long findLanguageIdByName(String languageName);

    void insertUserLearningLanguage(@Param("userId") Long userId, @Param("languageId") Long languageId);

    List<LanguageDto> findAllLanguages();

    List<String> findLanguageNamesByUserId(@Param("userId") Long userId);

    void deleteByUserId(@Param("userId") Long userId);

    void insertUserLearningLanguagesByNames(@Param("userId") Long userId, @Param("names") List<String> names);
}
