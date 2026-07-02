document.addEventListener("DOMContentLoaded", () => {
  const grid = document.getElementById("report-grid");
  const emptyEl = document.getElementById("report-empty");
  const pendingCountEl = document.getElementById("pending-count");
  const pendingBreakdownEl = document.getElementById("pending-breakdown");
  const tabButtons = document.querySelectorAll(".tab-button");

  const TYPE_INFO = {
    QUESTION: { label: "질문", cssClass: "question" },
    ANSWER: { label: "답변", cssClass: "answer" },
    COMMENT: { label: "댓글", cssClass: "comment" },
  };

  let allReports = [];
  let currentType = "QUESTION";
  let lastCounts = { total: 0, QUESTION: 0, ANSWER: 0, COMMENT: 0 };

  function getCsrfHeaders() {
    const tokenEl = document.querySelector('meta[name="_csrf"]');
    const headerEl = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenEl || !headerEl) return {};
    return { [headerEl.content]: tokenEl.content };
  }

  function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
  }

  async function loadReports() {
    const [questions, answers, comments, counts] = await Promise.all([
      fetch("/api/reports/pending/questions").then((res) => res.json()),
      fetch("/api/reports/pending/answers").then((res) => res.json()),
      fetch("/api/reports/pending/comments").then((res) => res.json()),
      fetch("/api/reports/pending/count").then((res) => res.json()),
    ]);

    allReports = [...questions, ...answers, ...comments].sort(
      (a, b) => new Date(b.createdAt) - new Date(a.createdAt)
    );
    lastCounts = counts;
    render();
  }

  function render() {
    const counts = lastCounts;
    const filtered = allReports.filter((r) => r.targetType === currentType);

    grid.querySelectorAll(".report-card").forEach((card) => card.remove());
    grid.insertAdjacentHTML("afterbegin", filtered.map(renderCard).join(""));

    pendingCountEl.textContent = counts.total;
    pendingBreakdownEl.textContent = `질문 ${counts.QUESTION} / 답변 ${counts.ANSWER} / 댓글 ${counts.COMMENT}`;
    emptyEl.style.display = filtered.length === 0 ? "block" : "none";
  }

  function renderCard(report) {
    const type = TYPE_INFO[report.targetType] ?? {
      label: report.targetType,
      cssClass: "",
    };
    const time = new Date(report.createdAt).toLocaleString("ko-KR");

    const clickable = !report.isTargetDeleted && report.questionId;
    const deletedBadge = report.isTargetDeleted
      ? `<span class="report-deleted-badge">삭제된 콘텐츠</span>`
      : "";

    return `
      <div class="report-card" data-report-id="${report.reportId}">
        <div class="report-header">
          <span class="report-type-badge ${type.cssClass}">${type.label}</span>
          <span class="report-status pending">대기 중</span>
        </div>

        <div class="report-info">
          <div class="reporter-info">
            <div class="reporter-avatar">${escapeHtml(
              (report.reporterNickname ?? "?")[0]
            )}</div>
            <span class="reporter-name">${escapeHtml(
              report.reporterNickname ?? "알 수 없음"
            )} (신고자)</span>
          </div>
          <span class="report-time">신고 일시: ${time}</span>
        </div>

        <div class="report-reason${clickable ? " is-clickable" : ""}"${
      clickable ? ` data-question-id="${report.questionId}"` : ""
    }>
          <h3 class="reason-title">신고 사유</h3>
          <p class="reason-text">${escapeHtml(report.reason)}</p>
        </div>

        ${deletedBadge}

        <div class="report-actions">
          <button class="action-btn approve" data-report-id="${report.reportId}">승인 (삭제)</button>
          <button class="action-btn reject" data-report-id="${report.reportId}">반려 (유지)</button>
        </div>
      </div>
    `;
  }

  grid.addEventListener("click", async (e) => {
    const reason = e.target.closest(".report-reason.is-clickable");
    if (reason) {
      window.open(`/questions/${reason.dataset.questionId}`, "_blank");
      return;
    }

    const btn = e.target.closest(".action-btn");
    if (!btn) return;

    const reportId = btn.dataset.reportId;
    const isApprove = btn.classList.contains("approve");
    const action = isApprove ? "approve" : "reject";
    const label = isApprove ? "승인(콘텐츠 삭제)" : "반려(콘텐츠 유지)";

    if (!confirm(`이 신고를 ${label} 처리할까요?`)) return;

    const res = await fetch(`/api/reports/${reportId}/${action}`, {
      method: "PUT",
      headers: { ...getCsrfHeaders() },
    });

    if (!res.ok) {
      alert(await res.text());
      return;
    }

    await loadReports();
  });

  tabButtons.forEach((tab) => {
    tab.addEventListener("click", () => {
      tabButtons.forEach((t) => t.classList.remove("active"));
      tab.classList.add("active");
      currentType = tab.dataset.type;
      render();
    });
  });

  loadReports();
});
