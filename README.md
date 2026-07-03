# 잇말 (ITMAL)

> 언어 교환 파트너를 찾고 1:1 채팅으로 소통할 수 있는 웹 서비스

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 4.0, Spring Security, Spring WebSocket (STOMP) |
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

#### API
- `GET /api/users/{userId}` : 닉네임, 모국어 반환
- `GET /api/check/email` : 이메일 중복 확인
- `GET /api/check/nickname` : 닉네임 중복 확인

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
- 튜터 신청 목록 조회 및 승인/거절

---
