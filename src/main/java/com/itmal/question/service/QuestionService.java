package com.itmal.question.service;

import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import com.itmal.question.dto.LanguageDto;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.mapper.QuestionMapper;
import com.itmal.question.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import com.itmal.question.dto.QuestionAttachmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "m4a", "wav", "ogg", "aac",                                // 음성
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip"  // 문서
    );

    @Transactional
    public void writeQuestion(QuestionDto questionDto, List<MultipartFile> files) {
        validateFiles(files); // 저장 전에 먼저 검증

        // CKEditor 본문 HTML sanitize (저장형 XSS 방지)
        questionDto.setContent(HtmlSanitizer.clean(questionDto.getContent()));

        questionMapper.insertQuestion(questionDto);

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue; // 빈 파트 무시
                }
                saveAttachment(questionDto.getQuestionId(), file);
            }
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        List<MultipartFile> realFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .toList();

        if (realFiles.size() > MAX_FILE_COUNT) {
            throw new IllegalArgumentException("파일은 최대 " + MAX_FILE_COUNT + "개까지 첨부할 수 있습니다.");
        }

        for (MultipartFile file : realFiles) {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        file.getOriginalFilename() + ": 파일 크기가 50MB를 초과합니다.");
            }
            String ext = extractExtension(file.getOriginalFilename());
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException(
                        "허용되지 않는 파일 형식입니다: " + file.getOriginalFilename());
            }
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private void saveAttachment(Long questionId, MultipartFile file) {
        try {

            Path dir = Paths.get(uploadDir, "questions", String.valueOf(questionId));
            Files.createDirectories(dir);

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                originalName = "file";
            }
            originalName = StringUtils.cleanPath(originalName);

            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalName.substring(dot);
            }
            String storedName = UUID.randomUUID() + ext;

            Path target = dir.resolve(storedName);
            registerRollbackCleanup(target); // 트랜잭션 롤백 시 디스크에 쓴 파일도 삭제(고아 파일 방지)
            file.transferTo(target);

            String relativePath = "questions/" + questionId + "/" + storedName;

            QuestionAttachmentDto attachment = new QuestionAttachmentDto();
            attachment.setQuestionId(questionId);
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setFilePath(relativePath);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            questionMapper.insertAttachment(attachment);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    // 파일 기록 후 트랜잭션이 롤백되면, DB는 되돌아가도 디스크 파일은 남으므로 함께 삭제한다.
    private void registerRollbackCleanup(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("롤백 시 첨부 파일 삭제 실패: {}", target, e);
                    }
                }
            }
        });
    }

    // 특정 유저의 질문 목록 (나경님 요청)
    public List<QuestionDto> findByUserId(Long userId) {
        return questionMapper.findByUserId(userId);
    }

    // 전체 질문 목록
    public List<QuestionDto> findAllQuestions() {
        return questionMapper.findAllQuestions();
    }

    // 언어 목록
    public List<LanguageDto> findAllLanguages() {
        return questionMapper.findAllLanguages();
    }

    // 첨부파일
    public QuestionAttachmentDto findAttachment(Long id) {
        return questionMapper.findAttachmentById(id);
    }

    // 질문 상세 1건 (작성자 + 언어 포함)
    public QuestionDto findQuestionDetail(Long id) {
        QuestionDto question = questionMapper.findQuestionDetailById(id);
        if (question == null) {
            throw new ViewException(ErrorCode.QUESTION_NOT_FOUND); // 페이지 → 404 HTML
        }
        return question;
    }

    // 질문 상세 1건의 첨부파일 목록
    public List<QuestionAttachmentDto> findAttachments(Long questionId) {
        return questionMapper.findAttachmentsByQuestionId(questionId);
    }

    // 작성자가 올린 질문 총 개수 (사이드바)
    public int countUserQuestions(Long userId) {
        return questionMapper.countQuestionsByUserId(userId);
    }

    @Transactional
    public void increaseViewCount(Long id) {
        questionMapper.increaseViewCount(id);
    }

    // 관련 질문
    public List<QuestionDto> findRelatedQuestions(QuestionDto question) {
        return questionMapper.findRelatedQuestions(
                question.getQuestionId(),
                question.getLanguageId(),
                question.getCategory()
        );
    }


    //질문 삭제
    @Transactional
    public void deleteQuestion(Long questionId, Long loginUserId){
        QuestionDto question = questionMapper.findQuestionDetailById(questionId);
        if(question == null){
            throw new ViewException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if (!question.getUserId().equals(loginUserId)){
            throw new ViewException(ErrorCode.QUESTION_FORBIDDEN);
        }
        // 소유자 조건을 SQL 에 실어 영향 row 수로 최종 판정 (선조회와 무관한 이중 방어)
        int deleted = questionMapper.softDeleteQuestion(questionId, loginUserId);
        if (deleted == 0) {
            throw new ViewException(ErrorCode.QUESTION_FORBIDDEN);
        }
        questionMapper.softDeleteAttachmentsByQuestionId(questionId); // 질문 삭제 시 첨부도 함께 비활성화
    }

    //수정 폼에 기존에 있던 글 채우기
    public QuestionDto getQuestionForEdit(Long questionId, Long loginUserId){
        QuestionDto question = questionMapper.findQuestionDetailById(questionId);
        if(question == null){
            throw new ViewException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if(!question.getUserId().equals(loginUserId)){
            throw new ViewException(ErrorCode.QUESTION_FORBIDDEN);
        }
        return question;
    }

    // 실제 수정 반영 (자기 글만)
    @Transactional
    public void updateQuestion(QuestionDto dto, Long loginUserId, List<MultipartFile> newFiles,
                               List<Long> deleteAttachmentIds){
        QuestionDto question = questionMapper.findQuestionDetailById(dto.getQuestionId());
        if (question == null){
            throw new ViewException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if (!question.getUserId().equals(loginUserId)){
            throw new ViewException(ErrorCode.QUESTION_FORBIDDEN);
        }

        List<QuestionAttachmentDto> current = questionMapper.findAttachmentsByQuestionId(dto.getQuestionId());

        List<QuestionAttachmentDto> toDelete = current.stream()
                .filter(a->deleteAttachmentIds != null && deleteAttachmentIds.contains(a.getId())).toList();

        validateFiles(newFiles);
        List<MultipartFile> realNew = (newFiles == null) ? List.of() : newFiles
                                                                       .stream()
                                                                       .filter(f-> f != null && !f.isEmpty()).toList();

        int finalCount = current.size() - toDelete.size() + realNew.size();
        if (finalCount > MAX_FILE_COUNT) {
            throw new IllegalArgumentException("파일은 최대 " + MAX_FILE_COUNT + "개까지 첨부할 수 있습니다.");
        }

        dto.setContent(HtmlSanitizer.clean(dto.getContent()));
        int updated = questionMapper.updateQuestion(dto, loginUserId); // 소유자 조건을 SQL WHERE 절에 실어 판정
        if (updated == 0) {
            throw new ViewException(ErrorCode.QUESTION_FORBIDDEN);
        }

        for (QuestionAttachmentDto att : toDelete) {
            // 첨부 삭제도 부모 질문 조건을 SQL 에 실어, 조작된 id 가 다른 질문 첨부를 건드리지 못하게 한다
            questionMapper.softDeleteAttachment(att.getId(), dto.getQuestionId());
        }

        for (MultipartFile file : realNew) {
            saveAttachment(dto.getQuestionId(),file);
        }

    }

    // 파일첨부 삭제 스케쥴러용
    public void purgeDetachedAttachments() {
        List<QuestionAttachmentDto> targets = questionMapper.findAttachmentsToPurge();
        for (QuestionAttachmentDto att : targets) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir).resolve(att.getFilePath()));
                questionMapper.deleteAttachment(att.getId());
            } catch (Exception e) { // 파일 IO 예외 + DB 런타임 예외 모두 잡아 다음 대상으로 진행
                log.warn("첨부 정리 실패(다음 배치 재시도): id={}, path={}", att.getId(), att.getFilePath(), e);
            }
        }
    }


}
