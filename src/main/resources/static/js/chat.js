// ============ 전역 변수 ============
let stompClient = null;
let currentChatRoomId = null;
let currentChatRequestId = null;
let currentUserId = null;
let currentOtherUserId = null;
let messageSubscription = null;
let readSubscription = null;
let typingSubscription = null;
let typingTimeout = null;
let typingDebounce = null;
let allRooms = [];
const userCache = {};
let currentTab = 'all';
let messageRenderQueue = Promise.resolve();

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function authHeaders() {
    return csrfToken ? {[csrfHeader]: csrfToken} : {};
}

async function getUserInfo(userId) {
    if (userCache[userId]) return userCache[userId];
    try {
        const res = await fetch(`/api/users/${userId}`);
        const json = await res.json();
        userCache[userId] = {
            nickname: json.data.nickname,
            nativeLanguage: json.data.nativeLanguage
        };
        return userCache[userId];
    } catch (e) {
        console.error(`유저 ${userId} 정보 조회 실패`, e);
        return {nickname: `사용자 ${userId}`, nativeLanguage: null};
    }
}

function connectStomp() {
    return new Promise((resolve, reject) => {
        stompClient.connect(authHeaders(), resolve, reject);
    });
}

async function setupWebSocket() {
    let socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    await connectStomp();

    const _sub1 = stompClient.subscribe(`/topic/chat-rooms/${currentUserId}`, async function (message) {
        const data = JSON.parse(message.body);
        if (data.type === 'ROOM_CREATED') {
            await loadChatRooms();
        }
    });

    const _sub2 = stompClient.subscribe(`/topic/unread-count/${currentUserId}`, async function (message) {
        await loadChatRooms();
    });
}

async function initCurrentUser() {
    currentUserId = CURRENT_USER_ID;
    console.log(currentUserId);
    if (!currentUserId) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    try {
        await setupWebSocket();
    } catch (error) {
        console.error('WebSocket 연결 실패:', error);
        window.location.href = '/login';
        return;
    }

    try {
        await Promise.all([loadChatRooms(), loadChatRequests()]);
    } catch (error) {
        console.error('초기 데이터 로드 실패:', error);
    }
}

document.addEventListener('DOMContentLoaded', async function () {
    await initCurrentUser();

    const roomId = new URLSearchParams(location.search).get('roomId');
    if (roomId) {
        const room = allRooms.find(r => r.id === Number(roomId));
        if (room) {
            const item = document.querySelector(`.chat-item[data-room-id="${roomId}"]`);
            await enterChatRoom(room.id, room.chatRequestId, item);
        }
    }
});


async function switchTab(tab) {
    currentTab = tab;
    document.getElementById('tabAll').classList.toggle('active', tab === 'all');
    document.getElementById('tabUnread').classList.toggle('active', tab === 'unread');
    await applyFilters();
}

async function applyFilters() {
    const query = document.getElementById('chatSearch')?.value.trim().toLowerCase() || '';
    let rooms = [...allRooms];
    if (currentTab === 'unread') rooms = rooms.filter(r => r.unreadCount > 0);
    if (query) rooms = rooms.filter(r => (userCache[r.otherUserId]?.nickname || '').toLowerCase().includes(query));
    await displayChatRooms(rooms);
}

async function loadChatRooms() {
    try {
        const response = await fetch(`/api/chat-room/list`);
        allRooms = await response.json();
        if (!Array.isArray(allRooms)) allRooms = [];
        await Promise.all(allRooms.map(r => getUserInfo(r.otherUserId)));
        await applyFilters();
    } catch (error) {
        console.error('채팅 목록 로드 실패:', error);
    }
}

async function displayChatRooms(rooms) {
    const container = document.getElementById('chatList');
    container.innerHTML = '';

    if (rooms.length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: var(--on-surface-variant);">채팅이 없습니다.</p>';
        return;
    }

    for (const room of rooms) {
        const nickname = (await getUserInfo(room.otherUserId)).nickname;
        const element = document.createElement('div');
        element.className = 'chat-item';
        element.dataset.roomId = room.id;
        element.style.cursor = 'pointer';
        element.onclick = () => enterChatRoom(room.id, room.chatRequestId, element);

        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(nickname)}&length=1&background=random`;
        element.innerHTML = `
          <div class="chat-item-avatar">
            <img src="${avatarUrl}" alt="user" />
          </div>
          <div class="chat-item-content">
            <div class="chat-item-header">
              <span class="chat-item-name">${escapeHtml(nickname)}</span>
              <span class="chat-item-time">${formatTime(room.lastMessageAt)}</span>
            </div>
            <p class="chat-item-text" style="margin: 0;">${escapeHtml(room.lastMessage ?? '')}</p>
          </div>
          ${room.unreadCount > 0 && room.id !== currentChatRoomId ? `<span class="chat-badge">${room.unreadCount}</span>` : ''}
        `;
        container.appendChild(element);
    }
}

async function enterChatRoom(chatRoomId, chatRequestId, clickedElement) {
    try {
        // 이전 구독 먼저 해제 후 전역 상태 업데이트
        if (messageSubscription) {
            await messageSubscription.unsubscribe();
            messageSubscription = null;
        }
        if (readSubscription) {
            await readSubscription.unsubscribe();
            readSubscription = null;
        }
        if (typingSubscription) {
            await typingSubscription.unsubscribe();
            typingSubscription = null;
        }

        const response = await fetch(`/api/chat-room/${chatRoomId}`);
        if (!response.ok) throw new Error(`채팅방 조회 실패: ${response.status}`);
        const data = await response.json();

        currentChatRoomId = chatRoomId;
        currentChatRequestId = chatRequestId;

        document.getElementById('chatMain').style.display = 'flex';
        document.getElementById('noChat').style.display = 'none';
        currentOtherUserId = data.otherUserId;
        const otherNickname = (await getUserInfo(currentOtherUserId)).nickname;
        document.getElementById('chatUserName').textContent = otherNickname;
        document.getElementById('chatUserAvatar').src = `https://ui-avatars.com/api/?name=${encodeURIComponent(otherNickname)}&length=1&background=random`;
        document.getElementById('messageContainer').innerHTML = '';

        await displayMessages(data.messages);

        // 읽음 처리
        await fetch(`/api/chat-room/mark-as-read?chatRoomId=${chatRoomId}&chatRequestId=${chatRequestId}`, {
            method: 'POST',
            headers: authHeaders()
        });

        // 뱃지 초기화
        if (clickedElement) {
            const badge = clickedElement.querySelector('.chat-badge');
            if (badge) badge.remove();
        }

        if (stompClient && stompClient.connected) {
            messageSubscription = stompClient.subscribe(`/topic/user/${chatRoomId}`, function (message) {
                if (currentChatRoomId !== chatRoomId) return;
                const msg = JSON.parse(message.body);
                hideTypingIndicator();
                messageRenderQueue = messageRenderQueue
                    .then(() => addMessageToUI(msg))
                    .catch(err => console.error('메시지 렌더링 실패:', err));

                if (msg.senderId !== currentUserId) {
                    fetch(`/api/chat-room/mark-as-read?chatRoomId=${chatRoomId}&chatRequestId=${chatRequestId}`, {
                        method: 'POST',
                        headers: authHeaders()
                    });
                }
            });

            // 읽음 처리 구독 (상대방이 읽으면 "1" 제거)
            readSubscription = stompClient.subscribe(`/topic/read/${chatRoomId}`, function () {
                document.querySelectorAll('.read-indicator').forEach(el => el.remove());
            });

            // 입력 중 구독 (내 userId 토픽 → 상대방이 보낸 것만 수신)
            typingSubscription = stompClient.subscribe(`/topic/typing/${currentUserId}`, function (frame) {
                const data = JSON.parse(frame.body);
                if (data.senderId !== currentOtherUserId) return;
                showTypingIndicator();
                clearTimeout(typingTimeout);
                typingTimeout = setTimeout(hideTypingIndicator, 3000);
            });
        }

        document.querySelectorAll('.chat-item').forEach(item => item.classList.remove('active'));
        if (clickedElement) {
            clickedElement.classList.add('active');
        }
    } catch (error) {
        console.error('채팅방 진입 실패:', error);
        alert('채팅방 진입에 실패했습니다');
    }
}

async function displayMessages(messages) {
    const container = document.getElementById('messageContainer');
    container.innerHTML = '';
    for (const msg of messages) {
        await addMessageToUI(msg);
    }
    container.scrollTop = container.scrollHeight;
}

async function addMessageToUI(message) {
    const container = document.getElementById('messageContainer');
    const isOwn = message.senderId === currentUserId;

    const messageElement = document.createElement('div');
    messageElement.className = `message ${isOwn ? 'own' : ''}`;

    if (isOwn) {
        messageElement.innerHTML = `
          <div class="message-content" style="align-items: flex-end;">
            <div class="message-bubble own">
              <p style="margin: 0">${escapeHtml(message.content)}</p>
            </div>
            <span class="message-timestamp">${formatTime(message.createdAt)}</span>
          </div>
          ${message.isRead === false ? '<span class="read-indicator">1</span>' : ''}
        `;
    } else {
        const nickname = (await getUserInfo(message.senderId)).nickname;
        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(nickname)}&length=1&background=random`;
        messageElement.innerHTML = `
          <div class="message-avatar">
            <img src="${avatarUrl}" alt="user" />
          </div>
          <div class="message-content">
            <div class="message-bubble other">
              <p style="margin: 0">${escapeHtml(message.content)}</p>
            </div>
            <button class="translate-btn">번역</button>
            <span class="message-timestamp">${formatTime(message.createdAt)}</span>
          </div>
        `;
        const translateBtn = messageElement.querySelector('.translate-btn');
        translateBtn.addEventListener('click', () => translateMessage(translateBtn, message.content));
    }

    container.appendChild(messageElement);
    container.scrollTop = container.scrollHeight;
}

function showTypingIndicator() {
    const container = document.getElementById('messageContainer');
    if (document.getElementById('typingIndicator')) return;
    const el = document.createElement('div');
    el.id = 'typingIndicator';
    el.className = 'message';
    el.innerHTML = `
      <div class="message-avatar">
        <img src="${document.getElementById('chatUserAvatar').src}" alt="user" />
      </div>
      <div class="message-content">
        <div class="message-bubble other typing-bubble">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>
    `;
    container.appendChild(el);
    container.scrollTop = container.scrollHeight;
}

function hideTypingIndicator() {
    document.getElementById('typingIndicator')?.remove();
}

function showTranslateError(btn, message) {
    const content = btn.closest('.message-content');
    let errorEl = content.querySelector('.translate-error');
    if (!errorEl) {
        errorEl = document.createElement('span');
        errorEl.className = 'translate-error';
        btn.after(errorEl);
    }
    errorEl.textContent = message;
    btn.textContent = '번역';
    setTimeout(() => errorEl.remove(), 3000);
}

async function translateMessage(btn, text) {
    const bubble = btn.closest('.message-content').querySelector('.message-bubble p');

    if (btn.dataset.translated === 'true') {
        bubble.textContent = btn.dataset.original;
        btn.dataset.translated = 'false';
        btn.textContent = '번역';
        return;
    }

    btn.disabled = true;
    btn.textContent = '번역 중...';
    try {
        const res = await fetch('/api/papago/translate', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', ...authHeaders()},
            body: JSON.stringify({
                source: 'auto',
                target: (await getUserInfo(CURRENT_USER_ID)).nativeLanguage || 'ko',
                text
            })
        });
        const json = await res.json();
        if (!res.ok) {
            const errorMap = {
                'N2MT05': '원문과 모국어가 동일합니다.',
                'N2MT04': '지원하지 않는 언어입니다.',
                'N2MT08': '번역할 수 없는 내용입니다.',
                'N2MT99': '번역 서버 오류입니다.',
            };
            showTranslateError(btn, errorMap[json.code] || json.message || '번역에 실패했습니다.');
            return;
        }
        const translated = json.data?.translatedText;
        btn.dataset.original = bubble.textContent;
        btn.dataset.translated = 'true';
        bubble.textContent = translated;
        btn.textContent = '원문 보기';
    } catch {
        showTranslateError(btn, '번역에 실패했습니다.');
    } finally {
        btn.disabled = false;
    }
}

function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content || !currentChatRoomId) {
        return;
    }

    if (!stompClient || !stompClient.connected) {
        console.error('WebSocket 미연결');
        alert('WebSocket이 연결되지 않았습니다.');
        return;
    }

    const message = {
        chatRoomId: currentChatRoomId,
        senderId: currentUserId,
        content: content,
        isRead: false,
        type: 'CHAT'
    };

    stompClient.send('/app/chat', {}, JSON.stringify(message));
    updateChatListPreview(currentChatRoomId, content);

    input.value = '';
    input.style.height = 'auto';
}

function updateChatListPreview(chatRoomId, content) {
    const item = document.querySelector(`.chat-item[data-room-id="${chatRoomId}"]`);
    if (!item) return;
    const textEl = item.querySelector('.chat-item-text');
    const timeEl = item.querySelector('.chat-item-time');
    if (textEl) textEl.textContent = content;
    if (timeEl) timeEl.textContent = formatTime(new Date().toISOString());
}

async function leaveRoom() {
    if (!confirm('채팅방을 나가시겠습니까?')) return;

    try {
        if (messageSubscription) {
            await messageSubscription.unsubscribe();
            messageSubscription = null;
        }
        if (readSubscription) {
            await readSubscription.unsubscribe();
            readSubscription = null;
        }
        if (typingSubscription) {
            await typingSubscription.unsubscribe();
            typingSubscription = null;
        }
        if (typingDebounce) { clearTimeout(typingDebounce); typingDebounce = null; }
        if (typingTimeout) { clearTimeout(typingTimeout); typingTimeout = null; }
        currentOtherUserId = null;
        hideTypingIndicator();

        const response = await fetch(`/api/chat-room/${currentChatRoomId}/leave?chatRequestId=${currentChatRequestId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', ...authHeaders()}
        });
        if (!response.ok) {
            console.error('채팅방 나가기 실패:', response.status);
            alert('채팅방 나가기에 실패했습니다.');
            return;
        }

        document.getElementById('chatMain').style.display = 'none';
        document.getElementById('noChat').style.display = 'flex';
        currentChatRoomId = null;
        currentChatRequestId = null;
        await loadChatRooms();

    } catch (error) {
        console.error('채팅방 나가기 실패:', error);
        alert('채팅방 나가기에 실패했습니다.');
    }
}

async function loadChatRequests() {
    try {
        const response = await fetch('/api/chat-request/pending');
        const requests = await response.json();
        await displayChatRequests(Array.isArray(requests) ? requests : []);
    } catch (error) {
        console.error('채팅 요청 로드 실패:', error);
    }
}

async function displayChatRequests(requests) {
    const container = document.getElementById('chatRequestContainer');
    container.innerHTML = '';

    if (requests.length === 0) {
        container.innerHTML = '<p style="padding: 12px 16px; font-size: 13px; color: var(--on-surface-variant);">받은 요청이 없습니다.</p>';
        return;
    }

    for (const req of requests) {
        const nickname = (await getUserInfo(req.requesterId)).nickname;
        const element = document.createElement('div');
        element.className = 'chat-request-item';
        element.innerHTML = `
          <div class="chat-request-info">
            <span class="chat-request-sender">${escapeHtml(nickname)}</span>
            <p class="chat-request-message">${escapeHtml(req.introMessage)}</p>
          </div>
          <div class="chat-request-actions">
            <button class="btn-accept" onclick="acceptRequest(${req.chatRequestId})">수락</button>
            <button class="btn-reject" onclick="rejectRequest(${req.chatRequestId}, this)">거절</button>
          </div>
        `;
        container.appendChild(element);
    }
}

async function acceptRequest(chatRequestId) {
    try {
        const response = await fetch(`/api/chat-request/${chatRequestId}/accept`, {
            method: 'PUT',
            headers: authHeaders()
        });
        const data = await response.json();
        if (!response.ok || !data.chatRoomId) {
            console.error('채팅 요청 수락 실패:', data);
            return;
        }
        await loadChatRooms();
        await loadChatRequests();
        await enterChatRoom(data.chatRoomId, chatRequestId);
    } catch (error) {
        console.error('채팅 요청 수락 실패:', error);
    }
}

async function rejectRequest(chatRequestId, btn) {
    try {
        const response = await fetch(`/api/chat-request/${chatRequestId}/reject`, {method: 'PUT', headers: authHeaders()});
        if (!response.ok) {
            console.error('채팅 요청 거절 실패:', response.status);
            return;
        }
        btn.closest('.chat-request-item').remove();
    } catch (error) {
        console.error('채팅 요청 거절 실패:', error);
    }
}

function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    const today = new Date();

    if (date.toDateString() === today.toDateString()) {
        return date.toLocaleTimeString('ko-KR', {hour: '2-digit', minute: '2-digit'});
    }
    return date.toLocaleDateString('ko-KR');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

document.getElementById('chatSearch')?.addEventListener('input', applyFilters);

document.getElementById('messageInput')?.addEventListener('keypress', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

document.getElementById('messageInput')?.addEventListener('input', function () {
    this.style.height = 'auto';
    this.style.height = Math.min(this.scrollHeight, 150) + 'px';

    if (stompClient && stompClient.connected && currentOtherUserId) {
        clearTimeout(typingDebounce);
        typingDebounce = setTimeout(() => {
            stompClient.send(`/app/chat/typing/${currentOtherUserId}`, {}, '');
        }, 300);
    }
});

window.addEventListener('beforeunload', function () {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
});