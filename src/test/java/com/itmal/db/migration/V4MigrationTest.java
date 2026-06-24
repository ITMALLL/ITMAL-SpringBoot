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
 * Integration tests for Flyway V4 migration: notification and report tables.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class V4MigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long userId;

    @BeforeEach
    void setUpUser() {
        jdbc.update(
            "INSERT INTO users (email, password, nickname, role) VALUES ('v4user@test.com', 'pw', 'v4nick', 'ROLE_USER')"
        );
        userId = jdbc.queryForObject("SELECT MAX(user_id) FROM users", Long.class);
    }

    // =========================================================
    // notification table tests
    // =========================================================
    @Nested
    @DisplayName("notification table")
    class NotificationTableTests {

        @Test
        @DisplayName("notification table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "NOTIFICATION", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("notification_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('ANSWER', ?)", userId
            );
            long id1 = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);

            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('COMMENT', ?)", userId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);

            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("type NOT NULL constraint is enforced")
        void typeNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO notification (type, user_id) VALUES (NULL, ?)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO notification (type, user_id) VALUES ('ANSWER', NULL)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("is_read defaults to FALSE")
        void isReadDefault() {
            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('LIKE', ?)", userId
            );
            long id = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_read FROM notification WHERE notification_id = ?", id
            );
            // H2 BOOLEAN / BIT may return Boolean false or 0
            Object isRead = row.get("is_read");
            assertThat(isRead).isIn(false, Boolean.FALSE, 0, (short) 0);
        }

        @Test
        @DisplayName("created_at defaults to CURRENT_TIMESTAMP")
        void createdAtDefault() {
            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('ANSWER', ?)", userId
            );
            long id = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at FROM notification WHERE notification_id = ?", id
            );
            assertThat(row.get("created_at")).isNotNull();
        }

        @Test
        @DisplayName("optional fields (target_type, target_id) accept NULL")
        void optionalFieldsNullable() {
            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('SYSTEM', ?)", userId
            );
            long id = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT target_type, target_id FROM notification WHERE notification_id = ?", id
            );
            assertThat(row.get("target_type")).isNull();
            assertThat(row.get("target_id")).isNull();
        }

        @Test
        @DisplayName("target_type and target_id can be stored when provided")
        void targetFieldsStored() {
            jdbc.update(
                "INSERT INTO notification (type, target_type, target_id, user_id) VALUES ('LIKE', 'QUESTION', 42, ?)",
                userId
            );
            long id = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT target_type, target_id FROM notification WHERE notification_id = ?", id
            );
            assertThat(row.get("target_type")).isEqualTo("QUESTION");
            assertThat(((Number) row.get("target_id")).longValue()).isEqualTo(42L);
        }

        @Test
        @DisplayName("is_read can be updated to TRUE")
        void isReadCanBeUpdatedToTrue() {
            jdbc.update(
                "INSERT INTO notification (type, user_id) VALUES ('ANSWER', ?)", userId
            );
            long id = jdbc.queryForObject("SELECT MAX(notification_id) FROM notification", Long.class);
            jdbc.update("UPDATE notification SET is_read = TRUE WHERE notification_id = ?", id);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_read FROM notification WHERE notification_id = ?", id
            );
            Object isRead = row.get("is_read");
            assertThat(isRead).isIn(true, Boolean.TRUE, 1, (short) 1);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO notification (type, user_id) VALUES ('ANSWER', 999999)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("multiple notifications can be created for a single user")
        void multipleNotificationsForUser() {
            jdbc.update("INSERT INTO notification (type, user_id) VALUES ('ANSWER', ?)", userId);
            jdbc.update("INSERT INTO notification (type, user_id) VALUES ('COMMENT', ?)", userId);
            jdbc.update("INSERT INTO notification (type, user_id) VALUES ('LIKE', ?)", userId);

            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ?",
                Integer.class, userId
            );
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("all columns present in the notification table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("NOTIFICATION");
            assertThat(columns).containsExactlyInAnyOrder(
                "notification_id", "type", "target_type", "target_id",
                "is_read", "created_at", "user_id"
            );
        }
    }

    // =========================================================
    // report table tests
    // =========================================================
    @Nested
    @DisplayName("report table")
    class ReportTableTests {

        @Test
        @DisplayName("report table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "REPORT", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("report_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, 'Spam', 'PENDING', ?)",
                userId
            );
            long id1 = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);

            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('ANSWER', 2, 'Abuse', 'PENDING', ?)",
                userId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);

            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("target_type NOT NULL constraint is enforced")
        void targetTypeNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES (NULL, 1, 'r', 'PENDING', ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("target_id NOT NULL constraint is enforced")
        void targetIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', NULL, 'r', 'PENDING', ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("reason NOT NULL constraint is enforced")
        void reasonNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, NULL, 'PENDING', ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("status NOT NULL constraint is enforced")
        void statusNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, 'r', NULL, ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, 'r', 'PENDING', NULL)"
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("created_at defaults to CURRENT_TIMESTAMP")
        void createdAtDefault() {
            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, 'reason', 'PENDING', ?)",
                userId
            );
            long id = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at FROM report WHERE report_id = ?", id
            );
            assertThat(row.get("created_at")).isNotNull();
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, 'r', 'PENDING', 999999)"
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("valid report can be inserted and queried")
        void insertValidReport() {
            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('ANSWER', 5, 'Offensive content', 'PENDING', ?)",
                userId
            );
            long id = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT target_type, target_id, reason, status FROM report WHERE report_id = ?", id
            );
            assertThat(row.get("target_type")).isEqualTo("ANSWER");
            assertThat(((Number) row.get("target_id")).longValue()).isEqualTo(5L);
            assertThat(row.get("reason")).isEqualTo("Offensive content");
            assertThat(row.get("status")).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("status can be updated (e.g., from PENDING to RESOLVED)")
        void statusCanBeUpdated() {
            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('COMMENT', 10, 'Bad', 'PENDING', ?)",
                userId
            );
            long id = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);
            jdbc.update("UPDATE report SET status = 'RESOLVED' WHERE report_id = ?", id);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT status FROM report WHERE report_id = ?", id
            );
            assertThat(row.get("status")).isEqualTo("RESOLVED");
        }

        @Test
        @DisplayName("all columns present in the report table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("REPORT");
            assertThat(columns).containsExactlyInAnyOrder(
                "report_id", "target_type", "target_id", "reason",
                "status", "created_at", "user_id"
            );
        }

        @Test
        @DisplayName("reason field stores long TEXT content")
        void reasonStoresLongText() {
            String longReason = "X".repeat(1000);
            jdbc.update(
                "INSERT INTO report (target_type, target_id, reason, status, user_id) VALUES ('QUESTION', 1, ?, 'PENDING', ?)",
                longReason, userId
            );
            long id = jdbc.queryForObject("SELECT MAX(report_id) FROM report", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT reason FROM report WHERE report_id = ?", id
            );
            assertThat(row.get("reason").toString()).hasSize(1000);
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