package com.itmal.question.controller;

import com.itmal.auth.domain.CustomUserDetails;
import com.itmal.question.dto.*;
import com.itmal.question.service.QuestionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/questions")
public class QuestionController {

        private final QuestionService questionService;

        @Value("${file.upload-dir}")
        private String uploadDir;

        // 질문 생성 페이지 보여주기
        @GetMapping("/write")
        public String writeForm(Model model) {
            model.addAttribute("languages", questionService.findAllLanguages());
            model.addAttribute("categories", Category.values());
            model.addAttribute("targets", Target.values());
            return "question/write";
        }

        //질문 상세
        @GetMapping("/{id}")
        public String detail(@PathVariable Long id, Model model, HttpSession session,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
            // 1. 질문 상세 조회
            QuestionDto question = questionService.findQuestionDetail(id);

            // 2. 세션 기반 조회수 중복 방지 (세션당 1회 보장)
            boolean firstView;
            synchronized (session) { // 같은 세션의 동시 요청 직렬화
                @SuppressWarnings("unchecked")
                Set<Long> viewed = (Set<Long>) session.getAttribute("viewedQuestions");
                if (viewed == null) {
                    viewed = new HashSet<>();
                    session.setAttribute("viewedQuestions", viewed);
                }
                firstView = viewed.add(id); // 새로 추가됐을 때만 true
            }
            if (firstView) {
                questionService.increaseViewCount(id);
                question.setViewCount(question.getViewCount() + 1);
            }

            // 로그인 했고 본인이 쓴 글일때
            boolean isOwner = userDetails != null && userDetails.getUserId().equals(question.getUserId());

            // 3. 화면에 데이터 전달
            model.addAttribute("isOwner", isOwner);
            model.addAttribute("question", question);
            model.addAttribute("attachments", questionService.findAttachments(id));
            model.addAttribute("questionCount", questionService.countUserQuestions(question.getUserId()));
            model.addAttribute("relatedQuestions", questionService.findRelatedQuestions(question));
            model.addAttribute("currentUserId", userDetails != null ? userDetails.getUserId() : null);

            // 좋아요 초기 상태 (새로고침해도 눌린 상태 유지)
            Long loginUserId = userDetails != null ? userDetails.getUserId() : null;
            model.addAttribute("likeCount", questionService.countLikes(id));
            model.addAttribute("liked", questionService.hasLiked(id, loginUserId));

            return "question/detail";
        }

        // 질문 생성 처리
        @PostMapping("/write")
        public String createQuestion(QuestionDto questionDto,
                                     @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
            if (userDetails == null) {
                return "redirect:/login"; // 비로그인 접근 시 NPE 방지 (이중 방어)
            }
            questionDto.setUserId(userDetails.getUserId());
            questionService.writeQuestion(questionDto, files);
            return "redirect:/questions/list";
        }

        //질문 삭제
        @PostMapping("/{id}/delete")
        public String deleteQuestion(@PathVariable Long id,
                                     @AuthenticationPrincipal CustomUserDetails userDetails){
            if (userDetails == null){
                return "redirect:/login";
            }
            questionService.deleteQuestion(id,userDetails.getUserId());
            return "redirect:/questions/list";
        }

        //수정
        @GetMapping("/{id}/edit")
        public String editForm(@PathVariable Long id, Model model,
                               @AuthenticationPrincipal CustomUserDetails userDetails){

            if(userDetails == null){
                return "redirect:/login";
            }

            model.addAttribute("question",questionService.getQuestionForEdit(id,userDetails.getUserId()));
            model.addAttribute("languages",questionService.findAllLanguages());
            model.addAttribute("categories", Category.values());
            model.addAttribute("targets", Target.values());
            model.addAttribute("attachments", questionService.findAttachments(id));

            return "question/edit";
        }
        //수정 처리
        @PostMapping("/{id}/edit")
        public String updateQuestion(@PathVariable Long id, QuestionDto questionDto,
                                     @RequestParam(value = "files", required = false) List<MultipartFile> files,
                                     @RequestParam(value = "deleteAttachmentIds", required = false) List<Long> deleteAttachmentIds,
                                     @AuthenticationPrincipal CustomUserDetails userDetails){
            if (userDetails == null){
                return "redirect:/login";
            }

            questionDto.setQuestionId(id);
            questionService.updateQuestion(questionDto, userDetails.getUserId(), files, deleteAttachmentIds);

            return "redirect:/questions/" + id; //수정 후 상세화면으로 감
        }

        //질문 목록 조회페이지
        @GetMapping("/list")
        public String questionList(@ModelAttribute QuestionSearchDto search, Model model) {
            
            List<QuestionDto> questions = questionService.findQuestions(search);
            int totalCount = questionService.countQuestions(search);
            int totalPages = (int) Math.ceil((double) totalCount / search.getSize());

            // currentPage를 [1, totalPages] 범위로 클램프 (범위 밖 page 요청 방어)
            int currentPage = Math.min(search.getPage(), Math.max(totalPages, 1));

            // 페이지 번호 윈도잉: 현재 페이지 주변 최대 PAGE_WINDOW개만 노출
            final int PAGE_WINDOW = 5;
            int startPage = Math.max(1, currentPage - PAGE_WINDOW / 2);
            int endPage = Math.min(totalPages, startPage + PAGE_WINDOW - 1);
            startPage = Math.max(1, endPage - PAGE_WINDOW + 1);

            model.addAttribute("questions", questions);
            model.addAttribute("languages", questionService.findAllLanguages());
            model.addAttribute("categories", Category.values());
            model.addAttribute("targets", Target.values());
            model.addAttribute("search", search);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("currentPage", currentPage);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);

            return "question/list";
        }

        //파일 다운로드 (항상 attachment 로 내려받기)
        @GetMapping("/attachments/{id}/download")
        public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) throws IOException {
            return buildAttachmentResponse(id, "attachment");
        }

        //오디오 미리듣기용 스트리밍 (브라우저에서 바로 재생하도록 inline)
        @GetMapping("/attachments/{id}/stream")
        public ResponseEntity<Resource> streamAttachment(@PathVariable Long id) throws IOException {
            return buildAttachmentResponse(id, "inline");
        }

        private ResponseEntity<Resource> buildAttachmentResponse(Long id, String dispositionType) throws IOException {
            QuestionAttachmentDto attachment = questionService.findAttachment(id);
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }

            Path path = Paths.get(uploadDir).resolve(attachment.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = attachment.getFileType() != null
                    ? attachment.getFileType() : "application/octet-stream";
            String encodedName = URLEncoder.encode(attachment.getOriginalName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            dispositionType + "; filename*=UTF-8''" + encodedName)
                    .body(resource);
        }

        // CKEditor 본문 이미지 업로드 (커스텀 어댑터가 호출)
        // 성공: {"url": "..."} / 실패: {"error": {"message": "..."}}
        @PostMapping("/upload-image")
        @ResponseBody
        public ResponseEntity<?> uploadImage(@RequestParam("upload") MultipartFile upload,
                                             @AuthenticationPrincipal CustomUserDetails userDetails,
                                             HttpServletRequest request) {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", Map.of("message", "로그인이 필요합니다.")));
            }
            try {
                String storedName = questionService.saveEditorImage(upload);
                // 절대 URL 반환 (sanitizer 의 http/https 프로토콜 허용을 통과시키기 위함)
                String url = ServletUriComponentsBuilder.fromContextPath(request)
                        .path("/questions/editor-images/")
                        .path(storedName)
                        .toUriString();
                return ResponseEntity.ok(Map.of("url", url));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", Map.of("message", e.getMessage())));
            }
        }

        // 본문 이미지 서빙 (inline)
        @GetMapping("/editor-images/{filename:.+}")
        public ResponseEntity<Resource> serveEditorImage(@PathVariable String filename) throws IOException {
            Path baseDir = Paths.get(uploadDir, "editor-images").normalize();
            Path path = baseDir.resolve(filename).normalize();
            // 경로 조작(../) 방어: baseDir 밖으로 벗어나면 거부
            if (!path.startsWith(baseDir)) {
                return ResponseEntity.badRequest().build();
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            MediaType contentType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(resource);
        }

        // 파일 검증 실패 시 400 + 메시지 반환
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleInvalidFile(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }


}
