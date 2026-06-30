package com.itmal.notification.mapper;

import com.itmal.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    List<Notification> findById(Long userId);

    Long findAnswerId(Long answerId);

    Long findQuestionId(Long questionId);

    Long findQuestionOwnerByAnswerId(Long answerId);

    int updateRead(@org.apache.ibatis.annotations.Param("notificationId") Long notificationId,
                   @org.apache.ibatis.annotations.Param("userId") Long userId);

    int updateAllRead(Long userId);

    int countUnread(Long userId);




}
