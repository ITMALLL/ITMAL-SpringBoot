package com.itmal.db.migration;

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
 * Integration tests for Flyway V1 migration: language, users, user_learning_language tables.
 *
 * Each test method runs in its own transaction that is rolled back after execution,
 * ensuring a clean database state between tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class V1MigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    // =========================================================
    // Helper: insert a language row and return generated ID
    // =========================================================
    private long insertLanguage(String name) {
        jdbc.update("INSERT INTO language (language_name) VALUES (?)", name);
        return jdbc.queryForObject("SELECT MAX(language_id) FROM language", Long.class);
    }

    // Helper: insert a user row and return generated ID
    private long insertUser(String email, String password, String nickname, String role) {
        jdbc.update(
            "INSERT INTO users (email, password, nickname, role) VALUES (?, ?, ?, ?)",
            email, password, nickname, role
        );
        return jdbc.queryForObject("SELECT MAX(user_id) FROM users", Long.class);
    }

    // =========================================================
    // language table tests
    // =========================================================
    @Nested
    @DisplayName("language table")
    class LanguageTableTests {

        @Test
        @DisplayName("language table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "LANGUAGE", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("language_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            long id1 = insertLanguage("English");
            long id2 = insertLanguage("Korean");
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("language_name NOT NULL constraint is enforced")
        void languageNameNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO language (language_name) VALUES (NULL)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("language row can be inserted with valid data")
        void insertValidLanguage() {
            long id = insertLanguage("Japanese");
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT * FROM language WHERE language_id = ?", id
            );
            assertThat(row.get("language_name")).isEqualTo("Japanese");
        }

        @Test
        @DisplayName("language_name respects VARCHAR(20) length limit")
        void languageNameVarcharLength() {
            String twentyChars = "A".repeat(20);
            long id = insertLanguage(twentyChars);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT language_name FROM language WHERE language_id = ?", id
            );
            assertThat(row.get("language_name").toString()).hasSize(20);
        }
    }

    // =========================================================
    // users table tests
    // =========================================================
    @Nested
    @DisplayName("users table")
    class UsersTableTests {

        @Test
        @DisplayName("users table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "USERS", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("user_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            long id1 = insertUser("a@test.com", "pass1", "nick1", "ROLE_USER");
            long id2 = insertUser("b@test.com", "pass2", "nick2", "ROLE_USER");
            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("email NOT NULL constraint is enforced")
        void emailNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES (NULL, 'pw', 'nick', 'ROLE_USER')")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("password NOT NULL constraint is enforced")
        void passwordNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES ('x@test.com', NULL, 'nicknull', 'ROLE_USER')")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("nickname NOT NULL constraint is enforced")
        void nicknameNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES ('y@test.com', 'pw', NULL, 'ROLE_USER')")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("role NOT NULL constraint is enforced")
        void roleNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES ('z@test.com', 'pw', 'nicknr', NULL)")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("email UNIQUE constraint is enforced")
        void emailUnique() {
            insertUser("dup@test.com", "pw", "nick_a", "ROLE_USER");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES ('dup@test.com', 'pw2', 'nick_b', 'ROLE_USER')")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("nickname UNIQUE constraint is enforced")
        void nicknameUnique() {
            insertUser("u1@test.com", "pw", "shared_nick", "ROLE_USER");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO users (email, password, nickname, role) VALUES ('u2@test.com', 'pw', 'shared_nick', 'ROLE_USER')")
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("email_verified defaults to 0 when not specified")
        void emailVerifiedDefault() {
            long id = insertUser("def@test.com", "pw", "defnick", "ROLE_USER");
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT email_verified FROM users WHERE user_id = ?", id
            );
            // H2 returns TINYINT(1) as Integer 0
            assertThat(((Number) row.get("email_verified")).intValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("created_at and updated_at are set by DEFAULT CURRENT_TIMESTAMP")
        void timestampDefaults() {
            long id = insertUser("ts@test.com", "pw", "tsnick", "ROLE_USER");
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at FROM users WHERE user_id = ?", id
            );
            assertThat(row.get("created_at")).isNotNull();
            assertThat(row.get("updated_at")).isNotNull();
        }

        @Test
        @DisplayName("nullable optional fields accept NULL")
        void optionalFieldsNullable() {
            long id = insertUser("opt@test.com", "pw", "optnick", "ROLE_USER");
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT native_language, provider, provider_id, deleted_at FROM users WHERE user_id = ?", id
            );
            assertThat(row.get("native_language")).isNull();
            assertThat(row.get("provider")).isNull();
            assertThat(row.get("provider_id")).isNull();
            assertThat(row.get("deleted_at")).isNull();
        }

        @Test
        @DisplayName("all four role ENUM values can be stored")
        void roleEnumValues() {
            String[] roles = {"ROLE_USER", "ROLE_TUTOR", "ROLE_MODERATOR", "ROLE_ADMIN"};
            for (int i = 0; i < roles.length; i++) {
                long id = insertUser("role" + i + "@test.com", "pw", "rolenick" + i, roles[i]);
                Map<String, Object> row = jdbc.queryForMap(
                    "SELECT role FROM users WHERE user_id = ?", id
                );
                assertThat(row.get("role").toString()).isEqualTo(roles[i]);
            }
        }

        @Test
        @DisplayName("all columns are present in the users table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("USERS");
            assertThat(columns).containsExactlyInAnyOrder(
                "user_id", "email", "password", "nickname", "role",
                "native_language", "provider", "provider_id",
                "email_verified", "deleted_at", "created_at", "updated_at"
            );
        }
    }

    // =========================================================
    // user_learning_language table tests
    // =========================================================
    @Nested
    @DisplayName("user_learning_language table")
    class UserLearningLanguageTableTests {

        @Test
        @DisplayName("user_learning_language table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "USER_LEARNING_LANGUAGE", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            long userId = insertUser("ull1@test.com", "pw", "ullnick1", "ROLE_USER");
            long langId = insertLanguage("Lang1");

            jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, ?)", userId, langId);
            long id1 = jdbc.queryForObject("SELECT MAX(id) FROM user_learning_language", Long.class);

            long userId2 = insertUser("ull2@test.com", "pw", "ullnick2", "ROLE_USER");
            long langId2 = insertLanguage("Lang2");
            jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, ?)", userId2, langId2);
            long id2 = jdbc.queryForObject("SELECT MAX(id) FROM user_learning_language", Long.class);

            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            long langId = insertLanguage("LangNullUser");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (NULL, ?)", langId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("language_id NOT NULL constraint is enforced")
        void languageIdNotNull() {
            long userId = insertUser("ull3@test.com", "pw", "ullnick3", "ROLE_USER");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, NULL)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            long langId = insertLanguage("LangForFk");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (999999, ?)", langId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to language is enforced")
        void foreignKeyToLanguage() {
            long userId = insertUser("ull4@test.com", "pw", "ullnick4", "ROLE_USER");
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, 999999)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("valid mapping row can be inserted and queried")
        void insertValidMapping() {
            long userId = insertUser("ull5@test.com", "pw", "ullnick5", "ROLE_USER");
            long langId = insertLanguage("Spanish");

            jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, ?)", userId, langId);
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM user_learning_language WHERE user_id = ? AND language_id = ?",
                userId, langId
            );
            assertThat(rows).hasSize(1);
        }

        @Test
        @DisplayName("a user can map to multiple languages")
        void userCanHaveMultipleLanguages() {
            long userId = insertUser("multi@test.com", "pw", "multinick", "ROLE_USER");
            long langId1 = insertLanguage("French");
            long langId2 = insertLanguage("German");

            jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, ?)", userId, langId1);
            jdbc.update("INSERT INTO user_learning_language (user_id, language_id) VALUES (?, ?)", userId, langId2);

            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_learning_language WHERE user_id = ?",
                Integer.class, userId
            );
            assertThat(count).isEqualTo(2);
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