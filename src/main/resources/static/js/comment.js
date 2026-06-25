document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".comments-section").forEach(initCommentSection);
});

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
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ content }),
    });

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
      });

      if (!res.ok) {
        alert(await res.text());
        return;
      }
      loadComments();
    }

    if (e.target.classList.contains("comment-edit")) {
      startEdit(e.target.closest(".comment-item"), commentId);
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
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content: newContent }),
      });

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

  //댓글 목록 불러오기
  async function loadComments() {
    const res = await fetch(`/api/answers/${answerId}/comments`);
    const comments = await res.json();
    listEl.innerHTML = comments.map(renderComment).join("");
  }

  function renderComment(comment) {
    const time = comment.updatedAt
      ? `${new Date(comment.updatedAt).toLocaleString("ko-KR")} (수정됨)`
      : new Date(comment.createdAt).toLocaleString("ko-KR");

    const nickname = escapeHtml(comment.nickname ?? "익명");
    const content = escapeHtml(comment.content);

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
            <button class="comment-edit" data-comment-id="${comment.commentId}">수정</button>
            <button class="comment-delete" data-comment-id="${comment.commentId}">삭제</button>
            <button class="comment-report" data-comment-id="${comment.commentId}">신고</button>
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
