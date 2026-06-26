// ============ 전역 변수 ============
let stompClient = null;
let currentChatRoomId = null;
let currentChatRequestId = null;
let chatRequestUsers = {};
let currentTab = 'all';

// URL 파라미터에서 userId 가져오기
const urlParams = new URLSearchParams(window.location.search);
let currentUserId = parseInt(urlParams.get('userId'));
// let currentUserId = [[${#authentication.principal.userId}]];


// ============ 초기화 ============
document.addEventListener('DOMContentLoaded', async function () {
    //요청 리스트 로드
    await loadChatRequests();
    //대화중인 리스트 로드
    await loadChatRooms();
    //웹소켓 세팅
    setupWebSocket();
    //현재 탭 버튼 클릭 리스너 등록
    setupTabListeners();
});

// ============ 채팅 요청 함 로드 ============
async function loadChatRequests() {
    try {
        const response = await fetch(`/api/chat-request/pending`);
        const requests = await response.json();
        displayChatRequests(requests);
    } catch (error) {
        console.error('채팅 요청 로드 실패:', error);
    }
}

// ============ 채팅 요청 표시 ============
function displayChatRequests(requests) {
    const container = document.getElementById('chatRequestContainer');
    container.innerHTML = '';

    if (requests.length === 0) return;

    requests.forEach(req => {
        chatRequestUsers[req.chatRequestId] = req.requesterId;

        const element = document.createElement('div');
        element.className = 'chat-item waiting';
        element.innerHTML = `
          <div class="chat-item-avatar">
            <img src="https://ui-avatars.com/api/?name=채원&background=0D8ABC" alt="requester" />
          </div>
          <div class="chat-item-content" style="flex: 1;">
            <div class="chat-item-header">
              <span class="chat-item-name">사용자 ${req.requesterId}</span>
              <span class="waiting-badge">수락 대기 중</span>
            </div>
            <p class="chat-item-message" style="margin: 0; font-size: 12px; opacity: 0.8; margin-bottom: 8px;">
              ${req.introMessage}
            </p>
            <div style="display: flex; gap: 8px;">
              <button onclick="acceptRequest(${req.chatRequestId})"
                      style="flex: 1; padding: 6px 8px; background: var(--primary); color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">
                수락
              </button>
              <button onclick="rejectRequest(${req.chatRequestId})"
                      style="flex: 1; padding: 6px 8px; background: var(--outline); color: var(--on-surface); border: none; border-radius: 4px; cursor: pointer; font-size: 12px; font-weight: 600;">
                거절
              </button>
            </div>
          </div>
        `;
        container.appendChild(element);
    });
}

// ============ 요청 수락 ============
async function acceptRequest(chatRequestId) {
    try {
        const response = await fetch(`/api/chat-request/${chatRequestId}/accept`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();
        if (data.chatRoomId) {
            await enterChatRoom(data.chatRoomId, chatRequestId);
            await loadChatRequests();
            await loadChatRooms();
        }
    } catch (error) {
        console.error('요청 수락 실패:', error);
        alert('요청 수락에 실패했습니다.');
    }
}

// ============ 요청 거절 ============
async function rejectRequest(chatRequestId) {
    try {
        await fetch(`/api/chat-request/${chatRequestId}/reject`, {
            method: 'PUT'
        });
        await loadChatRequests();
    } catch (error) {
        console.error('요청 거절 실패:', error);
        alert('요청 거절에 실패했습니다.');
    }
}

// ============ WebSocket 설정 ============
function setupWebSocket() {
    let socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {
        console.log('WebSocket 연결됨');
    }, function (error) {
        console.error('WebSocket 연결 실패:', error);
    });
}

// ============ 채팅방 진입 ============
async function enterChatRoom(chatRoomId, chatRequestId) {
    try {
        currentChatRoomId = chatRoomId;
        currentChatRequestId = chatRequestId;

        const response = await fetch(`/api/chat-room/${chatRoomId}`);
        const data = await response.json();

        document.getElementById('chatMain').style.display = 'flex';
        document.getElementById('noChat').style.display = 'none';
        document.getElementById('chatUserName').textContent = `사용자 ${data.responderInfo.requesterId === currentUserId ? data.responderInfo.responderId : data.responderInfo.requesterId}`;
        document.getElementById('messageContainer').innerHTML = '';

        displayMessages(data.messages);

        if (stompClient && stompClient.connected) {
            await stompClient.subscribe(`/topic/user/${chatRoomId}`, function (message) {
                const msg = JSON.parse(message.body);
                addMessageToUI(msg);
            });

            await stompClient.subscribe(`/topic/chat-read/${chatRoomId}`, function (message) {
                const data = JSON.parse(message.body);
                if (data.userId !== currentUserId) {
                    markOwnMessagesAsRead();
                }
            });
        }

        await fetch(`/api/chat-room/mark-as-read?chatRoomId=${chatRoomId}&chatRequestId=${chatRequestId}`, {
            method: 'POST'
        });

        document.querySelectorAll('.chat-item').forEach(item => item.classList.remove('active'));
        if (event && event.currentTarget) {
            event.currentTarget.classList.add('active');
        }
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
function addMessageToUI(message) {
    const container = document.getElementById('messageContainer');
    const isOwn = message.senderId === currentUserId;

    const messageElement = document.createElement('div');
    messageElement.className = `message ${isOwn ? 'own' : ''}`;

    if (isOwn) {
        const statusText = message.isRead ? '읽음' : '1';
        messageElement.innerHTML = `
          <div class="message-content" style="align-items: flex-end;">
            <div style="display: flex; align-items: flex-end; gap: 8px;">
              <span class="message-status">${statusText}</span>
              <div class="message-bubble own">
                <p style="margin: 0">${escapeHtml(message.content)}</p>
              </div>
            </div>
            <span class="message-timestamp">${formatTime(message.createdAt)}</span>
          </div>
        `;
    } else {
        messageElement.innerHTML = `
          <div class="message-avatar">
            <img src="https://ui-avatars.com/api/?name=User${message.senderId}&background=random" alt="user" />
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

// ============ 내가 보낸 메시지 읽음 표시 ============
function markOwnMessagesAsRead() {
    const messages = document.querySelectorAll('.message.own .message-status');
    messages.forEach(status => {
        status.textContent = '읽음';
    });
}

// ============ 메시지 전송 ============
function sendMessage() {
    const input = document.getElementById('messageInput');
    const content = input.value.trim();

    if (!content || !currentChatRoomId) return;

    if (!stompClient || !stompClient.connected) {
        alert('WebSocket이 연결되지 않았습니다.');
        return;
    }

    const message = {
        chatRoomId: currentChatRoomId,
        senderId: currentUserId,
        content: content,
        type: 'CHAT'
    };

    stompClient.send('/app/chat', {}, JSON.stringify(message));
    input.value = '';
    input.style.height = 'auto';
}

// ============ 나가기 ============
async function leaveRoom() {
    if (!confirm('채팅방을 나가시겠습니까?')) return;

    try {
        await fetch(`/api/chat-room/${currentChatRoomId}/leave`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        document.getElementById('chatMain').style.display = 'none';
        document.getElementById('noChat').style.display = 'flex';
        currentChatRoomId = null;
        currentChatRequestId = null;

        loadChatRooms();
    } catch (error) {
        console.error('채팅방 나가기 실패:', error);
        alert('채팅방 나가기에 실패했습니다.');
    }
}

// ============ 탭 설정 ============
function setupTabListeners() {
    document.querySelectorAll('.chat-tab-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            document.querySelectorAll('.chat-tab-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentTab = this.textContent.includes('안') ? '안 읽은 채팅' : '모든 채팅';
            loadChatRooms();
        });
    });
}

// ============ 채팅 목록 로드 ============
async function loadChatRooms() {
    try {
        const response = await fetch(`/api/chat-room/user`);  // ← 파라미터 제거!
        const rooms = await response.json();

        let filteredRooms = rooms;
        if (currentTab === 'unread') {
            filteredRooms = rooms.filter(room => room.unreadCount > 0);
        }

        displayChatRooms(filteredRooms);
    } catch (error) {
        console.error('채팅 목록 로드 실패:', error);
    }
}

// ============ 채팅 목록 표시 ============
function displayChatRooms(rooms) {
    const container = document.getElementById('chatList');
    container.innerHTML = '';

    if (rooms.length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: var(--on-surface-variant);">채팅이 없습니다.</p>';
        return;
    }

    rooms.forEach(room => {
        const element = document.createElement('div');
        element.className = 'chat-item';
        element.style.cursor = 'pointer';
        element.onclick = () => enterChatRoom(room.id, room.chatRequestId);

        const avatarUrl = `https://ui-avatars.com/api/?name=User&background=random`;
        element.innerHTML = `
          <div class="chat-item-avatar">
            <img src="${avatarUrl}" alt="user" />
          </div>
          <div class="chat-item-content">
            <div class="chat-item-header">
              <span class="chat-item-name">사용자 ID</span>
              <span class="chat-item-time">${formatTime(room.lastMessageAt)}</span>
            </div>
            <p class="chat-item-text" style="margin: 0;">마지막 메시지...</p>
          </div>
          ${room.unreadCount > 0 ? `<div class="chat-badge">${room.unreadCount}</div>` : ''}
        `;
        container.appendChild(element);
    });
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

// ============ 이벤트 리스너 ============
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