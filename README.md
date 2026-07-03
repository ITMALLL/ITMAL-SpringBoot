# 잇말 (ITMAL)

> 언어 교환 파트너를 찾고 1:1 채팅으로 소통할 수 있는 웹 서비스

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.0, Spring Security, Spring WebSocket (STOMP), SSE (Server-Sent Events) |
| Frontend | Thymeleaf, HTML / CSS / JavaScript |
| Database | MySQL, Flyway, MyBatis |
| 인증 | Spring Security, OAuth2 |
| 번역 | Papago API |

---

## 주요 기능

### 회원 (Auth)

#### 회원가입
- 이메일 실시간 중복 확인 및 인증 코드 발송
- 인증 코드 5분 유효 / 재발송 30초 쿨다운
- 모국어 및 학습 언어 선택 후 저장

#### 로그인
- Spring Security 기반 폼 로그인
- Google / GitHub OAuth2 소셜 로그인
  - 동일 이메일 기존 계정 자동 연결
  - 신규회원 : 모국어, 학습언어 추가 입력 페이지로 이동
  - 기존 회원 : 바로 로그인
- 탈퇴 계정 로그인 차단
- 로그인 유지 (Remember-Me,14일)

- 비밀번호 찾기
  - 이메일로 재설정 링크 발송
  - 단일 소비 토큰 (재사용 불가)
  - 새 비밀번호 BCrypt 인코딩 저장

- 회원 탈퇴
  - Soft Delete (deleted_at 기록)
  - 탈퇴 후 15일간 동일 이메일 재가입 차단

#### 권한 관리 
- ROLE_USER / ROLE_TUTOR / ROLE_ADMIN 분리
- 권한별 접근 제어 (Spring Security)
- 비로그인 접근 시 로그인 페이지 리다이렉트 


#### 마이페이지
- 닉네임 · 모국어 · 학습언어 수정 (수정 즉시 세션 갱신)
- 비밀번호 변경 (현재 비밀번호 검증 후 BCrypt 인코딩 저장)
- 회원 탈퇴 (Soft Delete - deleted_at 기록)

#### 회원 (Auth) API
  - `GET /api/users/{userId}` : 닉네임, 모국어 반환
  - `GET /api/check/email` : 이메일 중복 확인
  - `GET /api/check/nickname` : 닉네임 중복 확인
  - `POST /api/email/send` : 이메일 인증 코드 발송
  - `POST /api/email/verify` : 이메일 인증 코드 검증
#### 마이페이지 API
  - `GET /api/mypage/questions` : 내가 작성한 질문 목록
  - `GET /api/mypage/answers` : 내가 작성한 답변 목록
  - `GET /api/mypage/comment` : 내가 작성한 댓글 목록
  - `GET /api/mypage/countQuestions` : 작성 질문 수
  - `GET /api/mypage/countAnswers` : 작성 답변 수
  - `GET /api/mypage/countComments` : 작성 댓글 수

### 질문 & 답변
- 질문 작성 / 수정 / 삭제
- 파일 첨부 및 다운로드
- 질문 추천(좋아요) / 신고
- 답변 작성 / 수정 / 삭제
- 채택 기능 (베스트 답변 선정)
- 답변에 댓글 작성 / 조회

#### 질문 API
  - `GET /questions` : 질문 목록
  - `GET /questions/list` : 질문 검색/필터
  - `GET /questions/write` : 질문 작성 페이지
  - `POST /questions/write` : 질문 등록
  - `GET /questions/{id}` : 질문 상세
  - `GET /questions/{id}/edit` : 질문 수정 페이지
  - `POST /questions/{id}/edit` : 질문 수정
  - `POST /questions/{id}/delete` : 질문 삭제
  - `POST /questions/upload-image` : 에디터 이미지 업로드
  - `GET /questions/attachments/{id}/download` : 첨부파일 다운로드
  - `GET /questions/attachments/{id}/stream` : 오디오 미리듣기 스트리밍(inline)

#### 답변 API
  - `GET /answers` : 답변 목록
  - `POST /answers` : 답변 등록
  - `GET /answers/{id}/edit` : 답변 수정 페이지
  - `POST /answers/{id}/edit` : 답변 수정
  - `POST /answers/{id}/delete` : 답변 삭제
  - `POST /answers/{id}/like` : 답변 좋아요
  - `POST /answers/{id}/adopt` : 답변 채

### 1:1 채팅
- 채팅 요청 보내기 / 수락 / 거절
- WebSocket(STOMP) 기반 실시간 채팅
- 읽지 않은 메시지 뱃지 표시
- 채팅방 나가기
- 메시지 번역 (Papago API)

#### 채팅 API
  - `POST /api/chat-request` : 채팅 요청 전송
  - `GET /api/chat-request/{chatRequestId}` : 채팅 요청 조회
  - `GET /api/chat-request/pending` : 대기 중인 요청 목록
  - `PUT /api/chat-request/{id}/accept` : 채팅 요청 수락 (튜터 전용)
  - `PUT /api/chat-request/{id}/reject` : 채팅 요청 거절 (튜터 전용)
  - `GET /api/chat-room/list` : 채팅방 목록
  - `GET /api/chat-room/{chatRoomId}` : 채팅방 상세
  - `POST /api/chat-room/mark-as-read` : 읽음 처리
  - `POST /api/chat-room/{chatRoomId}/leave` : 채팅방 나가기
 #### 번역 API
  - `POST /api/papago/translate` : 메시지 번역

### 알림
- 작성한 질문 / 답변에 대한 실시간 알림 수신
- 읽음 처리

  #### 알림 API
  - `GET /api/notifications/sse` : SSE 연결
  - `GET /api/notifications` : 알림 목록
  - `GET /api/notifications/unread-count` : 안읽은 알림 수
  - `PUT /api/notifications/{id}/read` : 알림 읽음 처리
  - `PUT /api/notifications/read-all` : 전체 읽음 처리

### 신고
- 질문 · 답변 · 댓글 등 대상별 신고 접수
- 신고 사유 입력
  
  #### 신고 API
  - `POST /api/reports` : 신고 접수
  - `PUT /api/reports/{reportId}/approve` : 신고 승인 (관리자)
  - `PUT /api/reports/{reportId}/reject` : 신고 기각 (관리자)
  - `GET /api/reports/pending/questions` : 신고된 질문 목록
  - `GET /api/reports/pending/answers` : 신고된 답변 목록
  - `GET /api/reports/pending/comments` : 신고된 댓글 목록
  - `GET /api/reports/pending/count` : 미처리 신고 수

### 댓글

  #### 댓글 API
  - `GET /api/answers/{answerId}/comments` : 댓글 목록
  - `POST /api/answers/{answerId}/comments` : 댓글 등록
  - `PUT /api/comments/{commentId}` : 댓글 수정
  - `DELETE /api/comments/{commentId}` : 댓글 삭제

### 좋아요

  #### 좋아요 API
  - `POST /api/likes/{targetType}/{targetId}` : 좋아요 토글


### 관리자
- 관리자 전용 페이지
- 신고 내역 조회 및 처리
- 튜터 신청 목록 조회 및 승인/거절

#### 관리자 API
  - `GET /admin/tutor-applications` : 튜터 신청 목록
  - `POST /admin/tutor-applications/{id}/approve` : 튜터 승인
  - `POST /admin/tutor-applications/{id}/reject` : 튜터 거절
  - `GET /admin/tutors` : 튜터 목록
  - `POST /admin/tutors/{userId}/revoke` : 튜터 권한 박탈



---
