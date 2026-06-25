package com.itmal.question.controller;

import com.itmal.question.dto.QuestionDto;
import com.itmal.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.itmal.question.dto.QuestionAttachmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

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
            return "question/write";
        }

        // 질문 생성 처리
        @PostMapping("/write")
        public String createQuestion(QuestionDto questionDto,
                                     @RequestParam(value = "files", required = false) List<MultipartFile> files) {
            questionService.writeQuestion(questionDto, files);
            return "redirect:/questions/list";
        }

        //질문 목록 조회페이지
        @GetMapping("/list")
        public String questionList(Model model) {
            List<QuestionDto> questions = questionService.findAllQuestions();
            model.addAttribute("questions", questions);
            return "question/list";
        }

        //질문 상세 만들면 연결하기 (수정)
        @GetMapping("/attachments/{id}/download")
        public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) throws IOException {
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
                            "inline; filename*=UTF-8''" + encodedName)
                    .body(resource);
        }

        // 파일 검증 실패 시 400 + 메시지 반환
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<String> handleInvalidFile(IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
}
