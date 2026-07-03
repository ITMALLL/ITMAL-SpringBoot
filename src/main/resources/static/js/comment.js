document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".comments-section").forEach(initCommentSection);

  // 답변 카드가 나중에 동적으로(fetch 등으로) 추가돼도 자동으로 댓글 섹션 초기화
  const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType !== 1) return;
        if (node.matches?.(".comments-section")) initCommentSection(node);
        node.querySelectorAll?.(".comments-section").forEach(initCommentSection);
      });
    }
  });
  observer.observe(document.body, { childList: true, subtree: true });
});

function getCsrfHeaders() {
  const tokenEl = document.querySelector('meta[name="_csrf"]');
  const headerEl = document.querySelector('meta[name="_csrf_header"]');
  if (!tokenEl || !headerEl) return {};
  return { [headerEl.content]: tokenEl.content };
}

// 로그인 안 된 상태로 요청했을 때(401) 로그인 페이지로 보냄. 처리했으면 true 반환
function redirectIfUnauthenticated(res) {
  if (res.status === 401) {
    alert("로그인이 필요합니다.");
    window.location.href = "/login";
    return true;
  }
  return false;
}

// HTML에서 필요한 요소들 변수에 담기
function initCommentSection(section) {
  const answerId = section.dataset.answerId;
  const listEl = section.querySelector(".comments-list");
  const input = section.querySelector(".comment-field");
  const submitBtn = section.querySelector(".comment-input button");

  loadComments();
// 댓글 등록
  submitBtn.addEventListener("click", async () => {
    const content = input.value.trim();
    if (!content) return;

    const res = await fetch(`/api/answers/${answerId}/comments`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getCsrfHeaders() },
      body: JSON.stringify({ content }),
    });

    if (redirectIfUnauthenticated(res)) return;
    if (!res.ok) {
      alert(await res.text());
      return;
    }

    input.value = "";
    loadComments();
  });
// 댓글 삭제.수정 감지
  listEl.addEventListener("click", async (e) => {
    const commentId = e.target.dataset.commentId;
    if (!commentId) return;

    if (e.target.classList.contains("comment-delete")) {
      const ok = await showConfirmModal("댓글을 삭제할까요?");
      if (!ok) return;

      const res = await fetch(`/api/comments/${commentId}`, {
        method: "DELETE",
        headers: { ...getCsrfHeaders() },
      });

      if (redirectIfUnauthenticated(res)) return;
      if (!res.ok) {
        alert(await res.text());
        return;
      }
      loadComments();
    }

    if (e.target.classList.contains("comment-edit")) {
      startEdit(e.target.closest(".comment-item"), commentId);
    }

    if (e.target.classList.contains("comment-report")) {
      const reason = await showReportModal();
      if (!reason) return;

      const res = await fetch(`/api/reports`, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...getCsrfHeaders() },
        body: JSON.stringify({ targetType: "COMMENT", targetId: Number(commentId), reason }),
      });

      if (redirectIfUnauthenticated(res)) return;
      if (!res.ok) {
        let message = "신고에 실패했습니다.";
        try {
          const error = await res.json();
          if (error?.message) message = error.message;
        } catch (e) {
          // 서버가 JSON이 아닌 응답(에러 페이지 등)을 준 경우 - 기본 메시지 사용
        }
        await showInfoModal(message);
        return;
      }
      await showInfoModal("신고되었습니다.");
    }
  });

  function startEdit(commentItem, commentId) {
    const textEl = commentItem.querySelector(".comment-text");
    const actionsEl = commentItem.querySelector(".comment-actions");
    const currentContent = textEl.textContent;

    const editInput = document.createElement("input");
    editInput.type = "text";
    editInput.className = "comment-edit-input";
    editInput.value = currentContent;

    const editActions = document.createElement("div");
    editActions.className = "comment-edit-actions";

    const saveBtn = document.createElement("button");
    saveBtn.className = "comment-save";
    saveBtn.textContent = "저장";

    const cancelBtn = document.createElement("button");
    cancelBtn.className = "comment-cancel";
    cancelBtn.textContent = "취소";

    editActions.append(saveBtn, cancelBtn);

    textEl.replaceWith(editInput);
    actionsEl.style.display = "none";
    editInput.after(editActions);
    editInput.focus();

    saveBtn.addEventListener("click", async () => {
      const newContent = editInput.value.trim();
      if (!newContent) return;

      const res = await fetch(`/api/comments/${commentId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", ...getCsrfHeaders() },
        body: JSON.stringify({ content: newContent }),
      });

      if (redirectIfUnauthenticated(res)) return;
      if (!res.ok) {
        alert(await res.text());
        return;
      }
      loadComments();
    });

    cancelBtn.addEventListener("click", () => {
      loadComments();
    });
  }

  function showInfoModal(message) {
    return new Promise((resolve) => {
      const overlay = document.createElement("div");
      overlay.className = "comment-modal-overlay";
      overlay.innerHTML = `
        <div class="comment-modal">
          <p></p>
          <div class="comment-modal-actions">
            <button class="comment-modal-ok">확인</button>
          </div>
        </div>
      `;
      overlay.querySelector("p").textContent = message;
      document.body.appendChild(overlay);

      overlay.querySelector(".comment-modal-ok").addEventListener("click", () => {
        overlay.remove();
        resolve();
      });
    });
  }

  function showConfirmModal(message) {
    return new Promise((resolve) => {
      const overlay = document.createElement("div");
      overlay.className = "comment-modal-overlay";
      overlay.innerHTML = `
        <div class="comment-modal">
          <p></p>
          <div class="comment-modal-actions">
            <button class="comment-modal-cancel">취소</button>
            <button class="comment-modal-confirm">삭제</button>
          </div>
        </div>
      `;
      overlay.querySelector("p").textContent = message;
      document.body.appendChild(overlay);

      overlay.querySelector(".comment-modal-cancel").addEventListener("click", () => {
        overlay.remove();
        resolve(false);
      });
      overlay.querySelector(".comment-modal-confirm").addEventListener("click", () => {
        overlay.remove();
        resolve(true);
      });
    });
  }

  function showReportModal() {
    return new Promise((resolve) => {
      const reasonOptions = ["욕설/비하", "스팸", "광고", "허위정보", "부적절한 내용", "기타"];

      const overlay = document.createElement("div");
      overlay.className = "comment-modal-overlay";
      overlay.innerHTML = `
        <div class="comment-modal comment-report-modal">
          <div class="comment-report-modal-header">
            <span class="comment-report-modal-icon">🚨</span>
            <strong class="comment-report-modal-title">댓글 신고</strong>
          </div>
          <p class="comment-report-modal-subtitle">신고 사유를 선택해주세요. 허위 신고 시 제재를 받을 수 있습니다.</p>
          <div class="comment-report-reason-options">
            ${reasonOptions
              .map(
                (option, i) => `
              <label class="comment-report-reason-option">
                <input type="radio" name="report-reason" value="${option}" ${i === 0 ? "checked" : ""}>
                ${option}
              </label>`
              )
              .join("")}
          </div>
          <textarea class="comment-report-reason" rows="3" maxlength="200" placeholder="추가 설명 (선택, 최대 200자)"></textarea>
          <div class="comment-modal-actions">
            <button class="comment-modal-cancel">취소</button>
            <button class="comment-modal-confirm">신고하기</button>
          </div>
        </div>
      `;
      document.body.appendChild(overlay);

      const reasonInput = overlay.querySelector(".comment-report-reason");

      overlay.querySelector(".comment-modal-cancel").addEventListener("click", () => {
        overlay.remove();
        resolve(null);
      });
      overlay.querySelector(".comment-modal-confirm").addEventListener("click", () => {
        const selected = overlay.querySelector('input[name="report-reason"]:checked');
        const detail = reasonInput.value.trim();
        const reason = detail ? `${selected.value}: ${detail}` : selected.value;
        overlay.remove();
        resolve(reason);
      });
    });
  }

  //댓글 목록 불러오기
  async function loadComments() {
    const res = await fetch(`/api/answers/${answerId}/comments`);
    if (!res.ok) {
      listEl.innerHTML = "";
      return;
    }
    const comments = await res.json();
    listEl.innerHTML = comments.map(renderComment).join("");
  }

  function renderComment(comment) {
    const time = comment.updatedAt
      ? `${new Date(comment.updatedAt).toLocaleString("ko-KR")} (수정됨)`
      : new Date(comment.createdAt).toLocaleString("ko-KR");

    const nickname = escapeHtml(comment.nickname ?? "익명");
    const content = escapeHtml(comment.content);

    // 이 페이지에 currentUserId가 없는 경우(다른 페이지에서 재사용 등)를 대비한 안전장치
    const myUserId = typeof currentUserId !== "undefined" ? currentUserId : null;
    const isMine = myUserId != null && Number(comment.userId) === Number(myUserId);
    const actionsBtns = isMine
      ? `<button class="comment-edit" data-comment-id="${comment.commentId}">수정</button>
         <button class="comment-delete" data-comment-id="${comment.commentId}">삭제</button>`
      : `<button class="comment-report" data-comment-id="${comment.commentId}">신고</button>`;

    return `
      <div class="comment-item">
        <div class="comment-avatar">${nickname[0]}</div>
        <div class="comment-content">
          <div class="comment-author">
            ${nickname}
            <span class="comment-time">${time}</span>
          </div>
          <div class="comment-text">${content}</div>
          <div class="comment-actions">
            ${actionsBtns}
          </div>
        </div>
      </div>
    `;
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str;
    return div.innerHTML;
  }
}
