package com.itmal.notification.service;

import com.itmal.notification.domain.Notification;
import com.itmal.notification.dto.NotificationResponseDto;
import com.itmal.notification.mapper.NotificationMapper;
import com.itmal.notification.sse.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseEmitterRepository sseEmitterRepository;
    private final NotificationMapper notificationMapper;

    public SseEmitter subscribe(Long userId){
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseEmitterRepository.save(userId, emitter);
        emitter.onCompletion(() -> sseEmitterRepository.remove(userId));
        emitter.onTimeout(() -> sseEmitterRepository.remove(userId));
        return emitter;
    }

    public void createNotification(String type, String targetType, Long targetId, Long userId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setUserId(userId);

        notificationMapper.insert(notification);

        //저장해라
        SseEmitter emitter = sseEmitterRepository.get(userId);
        if (emitter != null) {
            try {
                int count = notificationMapper.countUnread(userId);
                emitter.send(SseEmitter.event().name("notification").data(count));
            } catch (IOException e) {
                sseEmitterRepository.remove(userId);
            }
        }
    }

    //질문알림
    public void createAnswerNotification(Long questionId) {
        Long userId = notificationMapper.findQuestionId(questionId);
        if (userId != null) {
            createNotification("ANSWER_CREATED", "QUESTION", questionId, userId);
        }
    }

    //댓글알림
    public void createCommentNotification(Long answerId, Long commentId) {
        Long userId = notificationMapper.findAnswerId(answerId);
        if (userId != null) {
            createNotification("COMMENT_CREATED", "COMMENT", commentId, userId);
        }
    }


    public List<NotificationResponseDto> getNotifications(Long userId) {
        List<Notification> notifications = notificationMapper.findById(userId);
        List<NotificationResponseDto> notificationResponseDtos = new ArrayList<>();

        for (Notification notification : notifications) {
            NotificationResponseDto dto = new NotificationResponseDto();
            dto.setNotificationId(notification.getNotificationId());
            dto.setTargetType(notification.getTargetType());
            dto.setTargetId(notification.getTargetId());
            dto.setIsRead(notification.getIsRead());
            dto.setCreatedAt(notification.getCreatedAt());

            if ("ANSWER_CREATED".equals(notification.getType())) {
                dto.setMessage("답변이 달렸습니다");
                dto.setQuestionTitle(notification.getQuestionTitle());
            } else if ("COMMENT_CREATED".equals(notification.getType())) {
                dto.setMessage("댓글이 달렸습니다");
                dto.setQuestionTitle(notification.getQuestionTitle());
                dto.setComment(notification.getComment());
            }

            notificationResponseDtos.add(dto);
        }
        return notificationResponseDtos;
    }


    public void updateRead(Long notificationId) {
        notificationMapper.updateRead(notificationId);
    }

    public void updateAllRead(Long userId) {
        notificationMapper.updateAllRead(userId);
    }

    public int countUnread(Long userId){
        return notificationMapper.countUnread(userId);
    }






}
