package com.itmal.tutorapplication.mapper;

import com.itmal.tutorapplication.domain.TutorApplication;
import com.itmal.tutorapplication.dto.TutorApplicationResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TutorApplicationMapper {
    void insert(TutorApplication application);
    TutorApplication findById(@Param("id") Long id);
    TutorApplication findPendingByUserId(@Param("userId") Long userId);
    List<TutorApplication> findAllPending();
    List<TutorApplicationResponse> findAllPendingWithUser();
    void updateStatus(TutorApplication application);
    void rejectPendingByUserId(@Param("userId") Long userId);
}
