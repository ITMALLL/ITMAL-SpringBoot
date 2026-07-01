package com.itmal.tutorapplication.mapper;

import com.itmal.tutorapplication.domain.TutorApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TutorApplicationMapper {
    void insert(TutorApplication application);
    TutorApplication findById(@Param("id") Long id);
    TutorApplication findPendingByUserId(@Param("userId") Long userId);
    List<TutorApplication> findAllPending();
    void updateStatus(TutorApplication application);
}
