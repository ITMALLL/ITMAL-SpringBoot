# 잇말 (ITMAL)

> 언어 교환 파트너를 찾고 1:1 채팅으로 소통할 수 있는 웹 서비스

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.0, Spring Security, Spring WebSocket (STOMP) |
| Frontend | Thymeleaf, HTML / CSS / JavaScript |
| Database | MySQL, Flyway |
| 인증 | Spring Security, OAuth2 |
| 번역 | Papago API |

---

## 주요 기능

### 회원
- 이메일 인증 기반 회원가입 / 로그인
- 소셜 로그인 (OAuth2)
- 닉네임 · 비밀번호 변경, 회원 탈퇴
- 마이페이지

### 질문 & 답변
- 질문 작성 / 수정 / 삭제
- 파일 첨부 및 다운로드
- 질문 추천(좋아요) / 신고
- 답변 작성 / 수정 / 삭제
- 채택 기능 (베스트 답변 선정)
- 답변에 댓글 작성 / 조회

### 1:1 채팅
- 채팅 요청 보내기 / 수락 / 거절
- WebSocket(STOMP) 기반 실시간 채팅
- 읽지 않은 메시지 뱃지 표시
- 채팅방 나가기
- 메시지 번역 (Papago API)

### 알림
- 채팅 요청, 답변 채택 등 주요 이벤트 알림
- 읽음 처리

### 신고
- 질문 · 답변 · 댓글 등 대상별 신고 접수
- 신고 사유 입력

### 관리자
- 관리자 전용 페이지
- 신고 내역 조회 및 처리

---
