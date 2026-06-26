package com.itmal.notification.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.notification.dto.NotificationResponseDto;
import com.itmal.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/sse")
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails){
        return notificationService.subscribe(userDetails.getUserId());
    }

    @GetMapping
    public List<NotificationResponseDto> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails){
        return notificationService.getNotifications(userDetails.getUserId());
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.updateRead(id);
    }

    @PutMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.updateAllRead(userDetails.getUserId());
    }

    @GetMapping("/unread-count")
    public int countUnread(@AuthenticationPrincipal CustomUserDetails userDetails){
        return notificationService.countUnread(userDetails.getUserId());
    }






}
