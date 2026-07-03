// static/js/like.js
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".like-btn").forEach(initLikeButton);
});

function getCsrfHeaders() {
    const tokenEl = document.querySelector('meta[name="_csrf"]');
    const headerEl = document.querySelector('meta[name="_csrf_header"]');
    if (!tokenEl || !headerEl) return {};
    return { [headerEl.content]: tokenEl.content };
}

function initLikeButton(button) {
    const targetType = button.dataset.targetType; // "ANSWER" | "QUESTION"
    const targetId = button.dataset.targetId;
    const heartIcon = button.querySelector(".heart-icon");
    const countEl = button.querySelector(".like-count");

    button.addEventListener("click", async () => {
        const wasLiked = button.classList.contains("liked");
        const prevCount = Number(countEl.textContent);

        // 클릭 즉시 반영 (optimistic update)
        applyState(!wasLiked, wasLiked ? prevCount - 1 : prevCount + 1);
        button.disabled = true;

        try {
            const res = await fetch(`/api/likes/${targetType}/${targetId}`, {
                method: "POST",
                headers: { ...getCsrfHeaders() },
            });
            if (res.status === 401) {
                applyState(wasLiked, prevCount);   // 낙관적 업데이트 되돌리기
                showLoginRequiredModal();
                return;
            }
            if (!res.ok) throw new Error(await res.text());

            const data = await res.json();
            applyState(data.liked, data.likeCount);
        } catch (e) {
            applyState(wasLiked, prevCount);
            alert("좋아요 처리에 실패했습니다.");
        } finally {
            button.disabled = false;
        }
    });

    function applyState(liked, count) {
        button.classList.toggle("liked", liked);
        heartIcon.textContent = liked ? "♥" : "♡";
        countEl.textContent = count;
    }
}

// 비로그인 상태에서 좋아요 클릭 시(401) 노출되는 로그인 안내 모달.
// like.js가 여러 페이지에서 공유되므로 마크업에 의존하지 않고 동적으로 생성한다.
function showLoginRequiredModal() {
    if (document.getElementById("like-login-modal")) return; // 중복 방지

    const overlay = document.createElement("div");
    overlay.id = "like-login-modal";
    overlay.style.cssText =
        "position:fixed;inset:0;z-index:9999;display:flex;align-items:center;justify-content:center;background:rgba(0,0,0,.45);";

    const box = document.createElement("div");
    box.style.cssText =
        "background:#fff;border-radius:16px;padding:28px 24px;max-width:320px;width:calc(100% - 40px);text-align:center;box-shadow:0 12px 40px rgba(0,0,0,.25);";
    box.innerHTML =
        '<div style="font-size:32px;line-height:1;margin-bottom:12px;">🔒</div>' +
        '<p style="font-size:16px;font-weight:600;color:#111;margin:0 0 6px;">로그인이 필요한 기능입니다.</p>' +
        '<p style="font-size:13px;color:#6b7280;margin:0 0 20px;">로그인 후 좋아요를 눌러보세요.</p>' +
        '<div style="display:flex;gap:8px;justify-content:center;">' +
        '  <button type="button" id="like-login-cancel" style="padding:8px 18px;border:1px solid #e5e7eb;border-radius:999px;background:#fff;color:#6b7280;font-size:14px;cursor:pointer;">닫기</button>' +
        '  <button type="button" id="like-login-go" style="padding:8px 18px;border:none;border-radius:999px;background:#2563eb;color:#fff;font-size:14px;cursor:pointer;">로그인 이동</button>' +
        '</div>';

    overlay.appendChild(box);
    document.body.appendChild(overlay);

    const close = () => overlay.remove();
    overlay.addEventListener("click", (e) => { if (e.target === overlay) close(); });
    box.querySelector("#like-login-cancel").addEventListener("click", close);
    box.querySelector("#like-login-go").addEventListener("click", () => {
        window.location.href = "/login";
    });
}