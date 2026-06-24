package com.itmal.db.migration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Flyway V3 migration: chat_request, chat_room, chat_message tables.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class V3MigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long requesterId;
    private long responderId;
    private long chatRequestId;

    @BeforeEach
    void setUpUsers() {
        jdbc.update(
            "INSERT INTO users (email, password, nickname, role) VALUES ('requester@test.com', 'pw', 'requester', 'ROLE_USER')"
        );
        requesterId = jdbc.queryForObject("SELECT MAX(user_id) FROM users", Long.class);

        jdbc.update(
            "INSERT INTO users (email, password, nickname, role) VALUES ('responder@test.com', 'pw', 'responder', 'ROLE_USER')"
        );
        responderId = jdbc.queryForObject("SELECT MAX(user_id) FROM users", Long.class);

        jdbc.update(
            "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, ?, 'PENDING')",
            requesterId, responderId
        );
        chatRequestId = jdbc.queryForObject("SELECT MAX(chat_request_id) FROM chat_request", Long.class);
    }

    // =========================================================
    // chat_request table tests
    // =========================================================
    @Nested
    @DisplayName("chat_request table")
    class ChatRequestTableTests {

        @Test
        @DisplayName("chat_request table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "CHAT_REQUEST", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("chat_request_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, ?, 'ACCEPTED')",
                requesterId, responderId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(chat_request_id) FROM chat_request", Long.class);
            assertThat(id2).isGreaterThan(chatRequestId);
        }

        @Test
        @DisplayName("requester_id NOT NULL constraint is enforced")
        void requesterIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (NULL, ?, 'PENDING')",
                    responderId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("responder_id NOT NULL constraint is enforced")
        void responderIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, NULL, 'PENDING')",
                    requesterId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("status NOT NULL constraint is enforced")
        void statusNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, ?, NULL)",
                    requesterId, responderId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all three status ENUM values are accepted")
        void statusEnumValues() {
            String[] statuses = {"PENDING", "ACCEPTED", "REJECTED"};
            for (String status : statuses) {
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, ?, ?)",
                    requesterId, responderId, status
                );
            }
            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM chat_request WHERE requester_id = ?",
                Integer.class, requesterId
            );
            // 1 from @BeforeEach + 3 above
            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("created_at defaults to CURRENT_TIMESTAMP")
        void createdAtDefault() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at FROM chat_request WHERE chat_request_id = ?", chatRequestId
            );
            assertThat(row.get("created_at")).isNotNull();
        }

        @Test
        @DisplayName("optional fields (intro_message, responded_at) accept NULL")
        void optionalFieldsNullable() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT intro_message, responded_at FROM chat_request WHERE chat_request_id = ?", chatRequestId
            );
            assertThat(row.get("intro_message")).isNull();
            assertThat(row.get("responded_at")).isNull();
        }

        @Test
        @DisplayName("intro_message can be stored and retrieved")
        void introMessageStored() {
            jdbc.update(
                "INSERT INTO chat_request (requester_id, responder_id, intro_message, status) VALUES (?, ?, 'Hello!', 'PENDING')",
                requesterId, responderId
            );
            long newId = jdbc.queryForObject("SELECT MAX(chat_request_id) FROM chat_request", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT intro_message FROM chat_request WHERE chat_request_id = ?", newId
            );
            assertThat(row.get("intro_message")).isEqualTo("Hello!");
        }

        @Test
        @DisplayName("foreign key requester_id to users is enforced")
        void foreignKeyRequesterToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (999999, ?, 'PENDING')",
                    responderId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key responder_id to users is enforced")
        void foreignKeyResponderToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, 999999, 'PENDING')",
                    requesterId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all columns present in the chat_request table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("CHAT_REQUEST");
            assertThat(columns).containsExactlyInAnyOrder(
                "chat_request_id", "requester_id", "responder_id",
                "intro_message", "status", "created_at", "responded_at"
            );
        }
    }

    // =========================================================
    // chat_room table tests
    // =========================================================
    @Nested
    @DisplayName("chat_room table")
    class ChatRoomTableTests {

        private long chatRoomId;

        @BeforeEach
        void setUpChatRoom() {
            jdbc.update(
                "INSERT INTO chat_room (chat_request_id) VALUES (?)", chatRequestId
            );
            chatRoomId = jdbc.queryForObject("SELECT MAX(id) FROM chat_room", Long.class);
        }

        @Test
        @DisplayName("chat_room table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "CHAT_ROOM", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            // Insert a second chat_request first
            jdbc.update(
                "INSERT INTO chat_request (requester_id, responder_id, status) VALUES (?, ?, 'PENDING')",
                requesterId, responderId
            );
            long newReqId = jdbc.queryForObject("SELECT MAX(chat_request_id) FROM chat_request", Long.class);

            jdbc.update("INSERT INTO chat_room (chat_request_id) VALUES (?)", newReqId);
            long id2 = jdbc.queryForObject("SELECT MAX(id) FROM chat_room", Long.class);
            assertThat(id2).isGreaterThan(chatRoomId);
        }

        @Test
        @DisplayName("chat_request_id NOT NULL constraint is enforced")
        void chatRequestIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO chat_room (chat_request_id) VALUES (NULL)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("created_at and updated_at default to CURRENT_TIMESTAMP")
        void timestampDefaults() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at FROM chat_room WHERE id = ?", chatRoomId
            );
            assertThat(row.get("created_at")).isNotNull();
            assertThat(row.get("updated_at")).isNotNull();
        }

        @Test
        @DisplayName("hidden_by_a and hidden_by_b default to FALSE")
        void hiddenByDefaults() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT hidden_by_a, hidden_by_b FROM chat_room WHERE id = ?", chatRoomId
            );
            assertThat(row.get("hidden_by_a")).isIn(false, Boolean.FALSE, 0, (short) 0);
            assertThat(row.get("hidden_by_b")).isIn(false, Boolean.FALSE, 0, (short) 0);
        }

        @Test
        @DisplayName("optional fields accept NULL")
        void optionalFieldsNullable() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT chat_title, last_message_at, last_read_at_a, last_read_at_b FROM chat_room WHERE id = ?",
                chatRoomId
            );
            assertThat(row.get("chat_title")).isNull();
            assertThat(row.get("last_message_at")).isNull();
            assertThat(row.get("last_read_at_a")).isNull();
            assertThat(row.get("last_read_at_b")).isNull();
        }

        @Test
        @DisplayName("foreign key to chat_request is enforced")
        void foreignKeyToChatRequest() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO chat_room (chat_request_id) VALUES (999999)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all columns present in the chat_room table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("CHAT_ROOM");
            assertThat(columns).containsExactlyInAnyOrder(
                "id", "chat_request_id", "chat_title", "created_at", "updated_at",
                "last_message_at", "last_read_at_a", "last_read_at_b",
                "hidden_by_a", "hidden_by_b"
            );
        }
    }

    // =========================================================
    // chat_message table tests
    // =========================================================
    @Nested
    @DisplayName("chat_message table")
    class ChatMessageTableTests {

        private long chatRoomId;

        @BeforeEach
        void setUpChatRoom() {
            jdbc.update(
                "INSERT INTO chat_room (chat_request_id) VALUES (?)", chatRequestId
            );
            chatRoomId = jdbc.queryForObject("SELECT MAX(id) FROM chat_room", Long.class);
        }

        @Test
        @DisplayName("chat_message table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "CHAT_MESSAGE", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("chat_message_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, ?, 'msg1')",
                chatRoomId, requesterId
            );
            long id1 = jdbc.queryForObject("SELECT MAX(chat_message_id) FROM chat_message", Long.class);

            jdbc.update(
                "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, ?, 'msg2')",
                chatRoomId, requesterId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(chat_message_id) FROM chat_message", Long.class);

            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("chat_room_id NOT NULL constraint is enforced")
        void chatRoomIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (NULL, ?, 'msg')",
                    requesterId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("sender_id NOT NULL constraint is enforced")
        void senderIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, NULL, 'msg')",
                    chatRoomId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("content NOT NULL constraint is enforced")
        void contentNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, ?, NULL)",
                    chatRoomId, requesterId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("created_at defaults to CURRENT_TIMESTAMP")
        void createdAtDefault() {
            jdbc.update(
                "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, ?, 'hello')",
                chatRoomId, requesterId
            );
            long id = jdbc.queryForObject("SELECT MAX(chat_message_id) FROM chat_message", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at FROM chat_message WHERE chat_message_id = ?", id
            );
            assertThat(row.get("created_at")).isNotNull();
        }

        @Test
        @DisplayName("foreign key to chat_room is enforced")
        void foreignKeyToChatRoom() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (999999, ?, 'msg')",
                    requesterId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key sender_id to users is enforced")
        void foreignKeySenderToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, 999999, 'msg')",
                    chatRoomId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("valid message can be inserted and queried")
        void insertValidMessage() {
            jdbc.update(
                "INSERT INTO chat_message (chat_room_id, sender_id, content) VALUES (?, ?, 'Hello World')",
                chatRoomId, requesterId
            );
            long id = jdbc.queryForObject("SELECT MAX(chat_message_id) FROM chat_message", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT content, sender_id FROM chat_message WHERE chat_message_id = ?", id
            );
            assertThat(row.get("content")).isEqualTo("Hello World");
            assertThat(((Number) row.get("sender_id")).longValue()).isEqualTo(requesterId);
        }

        @Test
        @DisplayName("all columns present in the chat_message table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("CHAT_MESSAGE");
            assertThat(columns).containsExactlyInAnyOrder(
                "chat_message_id", "chat_room_id", "sender_id", "content", "created_at"
            );
        }
    }

    // =========================================================
    // Utility
    // =========================================================
    private List<String> getColumnNames(String tableName) throws Exception {
        List<String> cols = new ArrayList<>();
        DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                cols.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
        return cols;
    }
}