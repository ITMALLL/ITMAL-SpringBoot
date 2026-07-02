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