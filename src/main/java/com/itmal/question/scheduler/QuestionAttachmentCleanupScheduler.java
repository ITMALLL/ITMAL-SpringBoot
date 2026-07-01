package com.itmal.question.scheduler;

import com.itmal.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionAttachmentCleanupScheduler {

    private final QuestionService questionService;

    // 매일 새벽 3시에 연결끊긴 첨부파일은 삭제하는 스케쥴러
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupDetachedAttachments() {
        questionService.purgeDetachedAttachments();
    }
}
