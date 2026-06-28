// ============ 전역 변수 ============
let stompClient = null;
let currentChatRoomId = null;
let currentChatRequestId = null;
let currentUserId = null;
let messageSubscription = null;
let readSubscription = null;
let allRooms = [];
const userCache = {};

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function authHeaders() {
    return csrfToken ? { [csrfHeader]: csrfToken } : {};
}

// ============ 유저 정보 조회 (캐시) ============
async function getUserNickname(userId) {
    if (userCache[userId]) return userCache[userId];
    try {
        const res = await fetch(`/api/users/${userId}`);
        const json = await res.json();
        userCache[userId] = json.data.nickname;
        return json.data.nickname;
    } catch {
        return `사용자 ${userId}`;
    }
}

// ============ WebSocket 설정 ============
function setupWebSocket() {
    return new Promise((resolve, reject) => {
        let socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);

        stompClient.connect(authHeaders(), function () {
            console.log('WebSocket 연결됨');

            // 채팅방 생성 알림 구독 (내가 요청자일 때 수락되면 받음)
            stompClient.subscribe(`/topic/chat-rooms/${currentUserId}`, function (message) {
                const data = JSON.parse(message.body);
                if (data.type === 'ROOM_CREATED') {
                    loadChatRooms();
                }
            });

            // 배지 업데이트 구독 (상대방이 메시지 보내면 목록 갱신)
            stompClient.subscribe(`/topic/unread-count/${currentUserId}`, function (message) {
                const data = JSON.parse(message.body);
                // 현재 열려있는 채팅방이면 배지 갱신 스킵
                if (data.chatRoomId === currentChatRoomId) return;
                loadChatRooms();
            });

            resolve();
        }, function (error) {
            console.error('WebSocket 연결 실패:', error);
            reject(error);
        });
    });
}

// ============ 초기화 ============
async function initCurrentUser() {
    try {
        const response = await fetch('/api/auth/me');
        const data = await response.json();
        currentUserId = data.userId;

        console.log('Current User ID:', currentUserId);

        if (!currentUserId) {
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
            return;
        }
        await setupWebSocket();
        await Promise.all([loadChatRooms(), loadChatRequests()]);

    } catch (error) {
        console.error('사용자 정보 조회 실패:', error);
        window.location.href = '/login';
    }
}

document.addEventListener('DOMContentLoaded', async function () {
    await initCurrentUser();
});

// ============ 탭 전환 ============
let currentTab = 'all';

function switchTab(tab) {
    currentTab = tab;
    document.getElementById('tabAll').classList.toggle('active', tab === 'all');
    document.getElementById('tabUnread').classList.toggle('active', tab === 'unread');
    applyFilters();
}

function applyFilters() {
    const query = document.getElementById('chatSearch')?.value.trim().toLowerCase() || '';
    let rooms = [...allRooms];
    if (currentTab === 'unread') rooms = rooms.filter(r => r.unreadCount > 0);
    if (query) rooms = rooms.filter(r => (userCache[r.otherUserId] || '').toLowerCase().includes(query));
    displayChatRooms(rooms);
}

// ============ 채팅 목록 로드 ============
async function loadChatRooms() {
    try {
        const response = await fetch(`/api/chat-room/list`);
        allRooms = await response.json();
        if (!Array.isArray(allRooms)) allRooms = [];
        // 닉네임 미리 캐싱
        await Promise.all(allRooms.map(r => getUserNickname(r.otherUserId)));
        applyFilters();
    } catch (error) {
        console.error('채팅 목록 로드 실패:', error);
    }
}

// ============ 채팅 목록 표시 ============
async function displayChatRooms(rooms) {
    const container = document.getElementById('chatList');
    container.innerHTML = '';

    if (rooms.length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: var(--on-surface-variant);">채팅이 없습니다.</p>';
        return;
    }

    for (const room of rooms) {
        const nickname = await getUserNickname(room.otherUserId);
        const element = document.createElement('div');
        element.className = 'chat-item';
        element.style.cursor = 'pointer';
        element.onclick = () => enterChatRoom(room.id, room.chatRequestId, element);

        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(nickname)}&background=random`;
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
          ${room.unreadCount > 0 ? `<span class="chat-badge">${room.unreadCount}</span>` : ''}
        `;
        container.appendChild(element);
    }
}

// ============ 채팅방 진입 ============
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

        currentChatRoomId = chatRoomId;
        currentChatRequestId = chatRequestId;

        const response = await fetch(`/api/chat-room/${chatRoomId}`);
        const data = await response.json();

        document.getElementById('chatMain').style.display = 'flex';
        document.getElementById('noChat').style.display = 'none';
        const otherUserId = data.otherUserId;
        document.getElementById('chatUserName').textContent = await getUserNickname(otherUserId);
        document.getElementById('messageContainer').innerHTML = '';

        displayMessages(data.messages);

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

        // 메시지 구독 (chatRequestId를 클로저로 캡처해 stale 참조 방지)
        if (stompClient && stompClient.connected) {
            const capturedRequestId = chatRequestId;
            messageSubscription = stompClient.subscribe(`/topic/user/${chatRoomId}`, function (message) {
                const msg = JSON.parse(message.body);
                addMessageToUI(msg);

                // 상대방 메시지 수신 시 즉시 읽음 처리
                if (msg.senderId !== currentUserId) {
                    fetch(`/api/chat-room/mark-as-read?chatRoomId=${chatRoomId}&chatRequestId=${capturedRequestId}`, {
                        method: 'POST',
                        headers: authHeaders()
                    });
                }
            });

            // 읽음 처리 구독 (상대방이 읽으면 "1" 제거)
            readSubscription = stompClient.subscribe(`/topic/read/${chatRoomId}`, function () {
                document.querySelectorAll('.read-indicator').forEach(el => el.remove());
            });
        }

        document.querySelectorAll('.chat-item').forEach(item => item.classList.remove('active'));
        if (clickedElement) {
            clickedElement.classList.add('active');
        }
        console.log('채팅방 진입 완료');
    } catch (error) {
        console.error('채팅방 진입 실패:', error);
        alert('채팅방 진입에 실패했습니다.');
    }
}

// ============ 메시지 목록 표시 ============
function displayMessages(messages) {
    const container = document.getElementById('messageContainer');
    container.innerHTML = '';
    messages.forEach(msg => addMessageToUI(msg));
    container.scrollTop = container.scrollHeight;
}

// ============ 메시지 추가 (UI) ============
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
            ${message.isRead === false ? '<span class="read-indicator">1</span>' : ''}
            <span class="message-timestamp">${formatTime(message.createdAt)}</span>
          </div>
        `;
    } else {
        const nickname = await getUserNickname(message.senderId);
        const avatarUrl = `https://ui-avatars.com/api/?name=${encodeURIComponent(nickname)}&background=random`;
        messageElement.innerHTML = `
          <div class="message-avatar">
            <img src="${avatarUrl}" alt="user" />
          </div>
          <div class="message-content">
            <div class="message-bubble other">
              <p style="margin: 0">${escapeHtml(message.content)}</p>
            </div>
            <span class="message-timestamp">${formatTime(message.createdAt)}</span>
          </div>
        `;
    }

    container.appendChild(messageElement);
    container.scrollTop = container.scrollHeight;
}

// ============ 메시지 전송 ============
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

    input.value = '';
    input.style.height = 'auto';
}

// ============ 채팅방 나가기 ============
async function leaveRoom() {
    if (!confirm('채팅방을 나가시겠습니까?')) return;

    try {
        await fetch(`/api/chat-room/${currentChatRoomId}/leave?chatRequestId=${currentChatRequestId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', ...authHeaders() }
        });

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

// ============ 유틸리티 ============
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

// ============ 채팅 요청함 ============
async function loadChatRequests() {
    try {
        const response = await fetch('/api/chat-request/pending');
        const requests = await response.json();
        displayChatRequests(Array.isArray(requests) ? requests : []);
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
        const nickname = await getUserNickname(req.requesterId);
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
        const response = await fetch(`/api/chat-request/${chatRequestId}/accept`, { method: 'PUT', headers: authHeaders() });
        const data = await response.json();
        await loadChatRooms();
        await loadChatRequests();
        enterChatRoom(data.chatRoomId, chatRequestId);
    } catch (error) {
        console.error('채팅 요청 수락 실패:', error);
    }
}

async function rejectRequest(chatRequestId, btn) {
    try {
        await fetch(`/api/chat-request/${chatRequestId}/reject`, { method: 'PUT', headers: authHeaders() });
        btn.closest('.chat-request-item').remove();
    } catch (error) {
        console.error('채팅 요청 거절 실패:', error);
    }
}

// ============ 이벤트 리스너 ============
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
});

window.addEventListener('beforeunload', function () {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
});