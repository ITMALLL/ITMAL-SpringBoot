package com.itmal.question.service;

import com.itmal.common.dto.LikeResponseDto;
import com.itmal.global.exception.ApiException;
import com.itmal.global.exception.ErrorCode;
import com.itmal.global.exception.ViewException;
import org.springframework.dao.DuplicateKeyException;
import com.itmal.papago.dto.PapagoRequestDto;
import com.itmal.papago.service.PapagoService;
import com.itmal.question.dto.LanguageDto;
import com.itmal.question.dto.LanguageStatDto;
import com.itmal.question.dto.QuestionDto;
import com.itmal.question.dto.QuestionSearchDto;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionMapper questionMapper;
    private final PapagoService papagoService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB
    private static final int TRANSLATION_MAX_CHARS = 5000; // translated_content 컬럼 및 Papago 입력 상한
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "m4a", "wav", "ogg", "aac",                                // 음성
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip"  // 문서
    );

    // CKEditor 본문 이미지 업로드 전용 (첨부파일과 별개)
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp"
    );
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final String EDITOR_IMAGE_DIR = "editor-images";
    // 본문 HTML에서 editor-images 파일명 추출용 (…/questions/editor-images/{파일명})
    private static final Pattern EDITOR_IMAGE_URL_PATTERN =
            Pattern.compile("/questions/editor-images/([^\"'\\s?>]+)");
    private static final long EDITOR_IMAGE_GRACE_HOURS = 24; // 첨부 정리와 동일한 1일 유예

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

        // 커밋 후 번역 캐시 워밍 (첫 조회자가 지연을 안 겪도록). 실패해도 조회 시 재시도됨
        registerTranslationWarmup(questionDto.getQuestionId(), questionDto.getContent(), questionDto.getLanguageId());
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

    /**
     * CKEditor 본문 이미지 저장 후 저장 파일명 반환.
     * 질문 생성 전(questionId 없음)에 호출되므로 editor-images 폴더에 저장한다.
     */
    public String saveEditorImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("이미지가 비어 있습니다.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 크기가 10MB를 초과합니다.");
        }
        // MIME 확인 (확장자 위조 방어)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다.");
        }
        try {
            Path dir = Paths.get(uploadDir, EDITOR_IMAGE_DIR);
            Files.createDirectories(dir);
            String storedName = UUID.randomUUID() + "." + ext;
            file.transferTo(dir.resolve(storedName));
            return storedName; // 컨트롤러에서 URL로 조립
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
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

    // 질문 검색필터용
    public List<QuestionDto> findQuestions(QuestionSearchDto search) {
        return questionMapper.findQuestions(search);
    }

    public int countQuestions(QuestionSearchDto search){
        return questionMapper.countQuestions(search);
    }

    // 언어 목록
    public List<LanguageDto> findAllLanguages() {
        return questionMapper.findAllLanguages();
    }

    // ── 홈 화면 데이터 ───────────────────────────────────────

    // 상단 통계 4개
    public long countAllQuestions() {
        return questionMapper.countAllQuestions();
    }

    public long countAllAnswers() {
        return questionMapper.countAllAnswers();
    }

    public long countActiveUsers() {
        return questionMapper.countActiveUsers();
    }

    public long countLanguages() {
        return questionMapper.countLanguages();
    }

    // 최근 질문 5건 (본문은 HTML 태그를 제거해 미리보기용 평문으로 변환)
    public List<QuestionDto> findRecentQuestions() {
        List<QuestionDto> questions = questionMapper.findRecentQuestions();
        for (QuestionDto q : questions) {
            q.setContent(HtmlSanitizer.toPlainText(q.getContent()));
        }
        return questions;
    }

    // 언어별 질문 현황 (진행률 바 너비를 가장 많은 언어 대비 상대값으로 계산)
    public List<LanguageStatDto> findLanguageStats() {
        List<LanguageStatDto> stats = questionMapper.countQuestionsByLanguage();
        long max = stats.stream().mapToLong(LanguageStatDto::getQuestionCount).max().orElse(0);
        for (LanguageStatDto stat : stats) {
            stat.setPercent(max == 0 ? 0 : (int) (stat.getQuestionCount() * 100 / max));
        }
        return stats;
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
        // 저장된 번역이 현재 대상 언어와 다르면(미번역/언어 변경/워밍 실패) 지금 번역해 캐시
        if (!Objects.equals(question.getLanguageCode(), question.getTranslatedTarget())) {
            String result = translateAndStore(
                    question.getQuestionId(), question.getContent(), question.getLanguageCode());
            if (result != null) { // null=실패/대상없음 → 캐시 미갱신, 다음 조회 재시도
                question.setTranslatedContent(result);
                question.setTranslatedTarget(question.getLanguageCode());
            }
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

    // ── 질문 좋아요 ─────────────────────────────────────────
    // 좋아요 토글 (있으면 취소, 없으면 추가) → 최신 liked/개수 반환
    @Transactional
    public LikeResponseDto toggleLike(Long questionId, Long userId) {
        boolean liked;
        if (questionMapper.existsQuestionLike(questionId, userId) > 0) {
            questionMapper.deleteQuestionLike(questionId, userId);
            liked = false;
        } else {
            try {
                questionMapper.insertQuestionLike(questionId, userId);
                liked = true;
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 중복 insert 시 무시 (이미 좋아요 상태로 간주)
                liked = true;
            }
        }
        return new LikeResponseDto(liked, questionMapper.countQuestionLikes(questionId));
    }

    // 현재 유저가 좋아요를 눌렀는지 (비로그인 false) — 상세 페이지 초기 상태용
    public boolean hasLiked(Long questionId, Long userId) {
        return userId != null && questionMapper.existsQuestionLike(questionId, userId) > 0;
    }

    // 좋아요 총 개수 (상세 페이지 초기 렌더용)
    public int countLikes(Long questionId) {
        return questionMapper.countQuestionLikes(questionId);
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

        // 본문이 바뀌었으므로 기존 번역 캐시는 위 update 에서 NULL 로 초기화됨 → 커밋 후 재번역 워밍
        registerTranslationWarmup(dto.getQuestionId(), dto.getContent(), dto.getLanguageId());
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

    // CKEditor 본문 이미지 고아 파일 정리 (스케줄러용)
    // 어떤 질문 본문에서도 참조하지 않고, 생성된 지 유예기간(24h)을 넘긴 파일만 삭제한다.
    public void purgeOrphanEditorImages() {
        Path dir = Paths.get(uploadDir, EDITOR_IMAGE_DIR);
        if (!Files.isDirectory(dir)) {
            return;
        }

        // 1) 현재 어떤 질문이든(소프트 삭제 포함) 본문에서 참조 중인 파일명 집합
        Set<String> referenced = new HashSet<>();
        for (String content : questionMapper.findContentsReferencingEditorImages()) {
            if (content == null) {
                continue;
            }
            Matcher m = EDITOR_IMAGE_URL_PATTERN.matcher(content);
            while (m.find()) {
                referenced.add(m.group(1));
            }
        }

        // 2) 폴더 내 파일 중 미참조 + 유예기간 경과 파일 삭제
        Instant threshold = Instant.now().minus(EDITOR_IMAGE_GRACE_HOURS, ChronoUnit.HOURS);
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                if (referenced.contains(name)) {
                    return; // 사용 중
                }
                try {
                    // 작성 중 업로드 보호: 최근 생성/수정된 파일은 건너뜀
                    if (Files.getLastModifiedTime(path).toInstant().isAfter(threshold)) {
                        return;
                    }
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("고아 이미지 정리 실패(다음 배치 재시도): {}", path, e);
                }
            });
        } catch (IOException e) {
            log.warn("editor-images 디렉터리 조회 실패", e);
        }
    }

    // ── 번역 캐시 ─────────────────────────────────────────────
    // 본문을 languageCode 로 번역해 DB에 저장. best-effort.
    // 반환: 번역문 / "" (원문=대상 동일 언어) / null (대상 없음·본문 없음·실패)
    private String translateAndStore(Long questionId, String content, String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return null;
        }
        String plain = HtmlSanitizer.toPlainText(content);
        if (plain == null || plain.isBlank()) {
            return null;
        }
        if (plain.length() > TRANSLATION_MAX_CHARS) {
            plain = plain.substring(0, TRANSLATION_MAX_CHARS);
        }
        try {
            String translated = papagoService.translate(new PapagoRequestDto("auto", languageCode, plain));
            if (translated != null && translated.length() > TRANSLATION_MAX_CHARS) {
                translated = translated.substring(0, TRANSLATION_MAX_CHARS); // 컬럼 상한 보호
            }
            questionMapper.updateTranslation(questionId, translated, languageCode);
            return translated;
        } catch (ApiException e) {
            if (ErrorCode.N2MT05 == e.getErrorCode()) {
                // 원문 언어 == 대상 언어 → 번역 불필요. 빈 문자열로 캐싱해 재호출 방지
                questionMapper.updateTranslation(questionId, "", languageCode);
                return "";
            }
            log.warn("질문 번역 실패(다음 조회 시 재시도): questionId={}, code={}", questionId, e.getErrorCode());
            return null;
        } catch (Exception e) {
            log.warn("질문 번역 실패(다음 조회 시 재시도): questionId={}", questionId, e);
            return null;
        }
    }

    // 쓰기/수정 커밋 후 번역 캐시를 채운다. 트랜잭션 밖(afterCommit)에서 실행해 HTTP 호출이 DB 트랜잭션을 잡지 않게 한다.
    private void registerTranslationWarmup(Long questionId, String content, Long languageId) {
        if (questionId == null || languageId == null) {
            return;
        }
        String languageCode = questionMapper.findLanguageCodeById(languageId);
        if (languageCode == null || languageCode.isBlank()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            translateAndStore(questionId, content, languageCode); // 트랜잭션 없으면 즉시 실행
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                translateAndStore(questionId, content, languageCode);
            }
        });
    }

}
