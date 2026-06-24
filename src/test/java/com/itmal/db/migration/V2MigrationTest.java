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
 * Integration tests for Flyway V2 migration:
 * question, question_attachment, question_like, answer, answer_like, comment tables.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class V2MigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    // IDs shared across nested test setups
    private long userId;
    private long languageId;
    private long questionId;

    @BeforeEach
    void setUpCommonEntities() {
        jdbc.update("INSERT INTO language (language_name) VALUES ('TestLang')");
        languageId = jdbc.queryForObject("SELECT MAX(language_id) FROM language", Long.class);

        jdbc.update(
            "INSERT INTO users (email, password, nickname, role) VALUES ('v2user@test.com', 'pw', 'v2nick', 'ROLE_USER')"
        );
        userId = jdbc.queryForObject("SELECT MAX(user_id) FROM users", Long.class);

        jdbc.update(
            "INSERT INTO question (title, content, user_id, language_id) VALUES ('Test Question', 'Test Content', ?, ?)",
            userId, languageId
        );
        questionId = jdbc.queryForObject("SELECT MAX(question_id) FROM question", Long.class);
    }

    // =========================================================
    // question table tests
    // =========================================================
    @Nested
    @DisplayName("question table")
    class QuestionTableTests {

        @Test
        @DisplayName("question table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "QUESTION", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("question_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO question (title, content, user_id, language_id) VALUES ('Q2', 'C2', ?, ?)",
                userId, languageId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(question_id) FROM question", Long.class);
            assertThat(id2).isGreaterThan(questionId);
        }

        @Test
        @DisplayName("title NOT NULL constraint is enforced")
        void titleNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES (NULL, 'c', ?, ?)",
                    userId, languageId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("content NOT NULL constraint is enforced")
        void contentNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES ('t', NULL, ?, ?)",
                    userId, languageId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES ('t', 'c', NULL, ?)",
                    languageId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("language_id NOT NULL constraint is enforced")
        void languageIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES ('t', 'c', ?, NULL)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("view_count defaults to 0")
        void viewCountDefault() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT view_count FROM question WHERE question_id = ?", questionId
            );
            assertThat(((Number) row.get("view_count")).longValue()).isEqualTo(0L);
        }

        @Test
        @DisplayName("created_at and updated_at default to CURRENT_TIMESTAMP")
        void timestampDefaults() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at FROM question WHERE question_id = ?", questionId
            );
            assertThat(row.get("created_at")).isNotNull();
            assertThat(row.get("updated_at")).isNotNull();
        }

        @Test
        @DisplayName("optional fields (category, deleted_at) accept NULL")
        void optionalFieldsNullable() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT category, deleted_at FROM question WHERE question_id = ?", questionId
            );
            assertThat(row.get("category")).isNull();
            assertThat(row.get("deleted_at")).isNull();
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES ('t', 'c', 999999, ?)",
                    languageId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to language is enforced")
        void foreignKeyToLanguage() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question (title, content, user_id, language_id) VALUES ('t', 'c', ?, 999999)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all columns are present in the question table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("QUESTION");
            assertThat(columns).containsExactlyInAnyOrder(
                "question_id", "title", "content", "view_count",
                "created_at", "updated_at", "category", "deleted_at",
                "user_id", "language_id"
            );
        }
    }

    // =========================================================
    // question_attachment table tests
    // =========================================================
    @Nested
    @DisplayName("question_attachment table")
    class QuestionAttachmentTableTests {

        @Test
        @DisplayName("question_attachment table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "QUESTION_ATTACHMENT", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("valid attachment can be inserted")
        void insertValidAttachment() {
            jdbc.update(
                "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES (?, ?, ?, ?)",
                "file.txt", "stored_file.txt", "/uploads/stored_file.txt", questionId
            );
            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM question_attachment WHERE question_id = ?",
                Integer.class, questionId
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("original_name NOT NULL constraint is enforced")
        void originalNameNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES (NULL, 'sn', '/p', ?)",
                    questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("stored_name NOT NULL constraint is enforced")
        void storedNameNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES ('on', NULL, '/p', ?)",
                    questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("file_path NOT NULL constraint is enforced")
        void filePathNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES ('on', 'sn', NULL, ?)",
                    questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to question is enforced")
        void foreignKeyToQuestion() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES ('on', 'sn', '/p', 999999)"
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("optional fields (file_type, file_size) accept NULL")
        void optionalFieldsNullable() {
            jdbc.update(
                "INSERT INTO question_attachment (original_name, stored_name, file_path, question_id) VALUES ('f', 's', '/p', ?)",
                questionId
            );
            long id = jdbc.queryForObject("SELECT MAX(id) FROM question_attachment", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT file_type, file_size FROM question_attachment WHERE id = ?", id
            );
            assertThat(row.get("file_type")).isNull();
            assertThat(row.get("file_size")).isNull();
        }
    }

    // =========================================================
    // question_like table tests
    // =========================================================
    @Nested
    @DisplayName("question_like table")
    class QuestionLikeTableTests {

        @Test
        @DisplayName("question_like table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "QUESTION_LIKE", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("valid question like can be inserted")
        void insertValidLike() {
            jdbc.update(
                "INSERT INTO question_like (user_id, question_id) VALUES (?, ?)",
                userId, questionId
            );
            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM question_like WHERE user_id = ? AND question_id = ?",
                Integer.class, userId, questionId
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO question_like (user_id, question_id) VALUES (NULL, ?)", questionId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("question_id NOT NULL constraint is enforced")
        void questionIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO question_like (user_id, question_id) VALUES (?, NULL)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO question_like (user_id, question_id) VALUES (999999, ?)", questionId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to question is enforced")
        void foreignKeyToQuestion() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO question_like (user_id, question_id) VALUES (?, 999999)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // =========================================================
    // answer table tests
    // =========================================================
    @Nested
    @DisplayName("answer table")
    class AnswerTableTests {

        private long answerId;

        @BeforeEach
        void setUpAnswer() {
            jdbc.update(
                "INSERT INTO answer (content, user_id, question_id) VALUES ('Answer content', ?, ?)",
                userId, questionId
            );
            answerId = jdbc.queryForObject("SELECT MAX(answer_id) FROM answer", Long.class);
        }

        @Test
        @DisplayName("answer table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "ANSWER", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("answer_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO answer (content, user_id, question_id) VALUES ('Another', ?, ?)",
                userId, questionId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(answer_id) FROM answer", Long.class);
            assertThat(id2).isGreaterThan(answerId);
        }

        @Test
        @DisplayName("content NOT NULL constraint is enforced")
        void contentNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO answer (content, user_id, question_id) VALUES (NULL, ?, ?)",
                    userId, questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("is_accepted defaults to 0")
        void isAcceptedDefault() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_accepted FROM answer WHERE answer_id = ?", answerId
            );
            assertThat(((Number) row.get("is_accepted")).intValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("like_count defaults to 0")
        void likeCountDefault() {
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT like_count FROM answer WHERE answer_id = ?", answerId
            );
            assertThat(((Number) row.get("like_count")).intValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("self-referencing FK (post_id) to parent answer works")
        void selfReferencingFk() {
            // Insert a child answer referencing the parent
            jdbc.update(
                "INSERT INTO answer (post_id, content, user_id, question_id) VALUES (?, 'Child answer', ?, ?)",
                answerId, userId, questionId
            );
            long childId = jdbc.queryForObject("SELECT MAX(answer_id) FROM answer", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT post_id FROM answer WHERE answer_id = ?", childId
            );
            assertThat(((Number) row.get("post_id")).longValue()).isEqualTo(answerId);
        }

        @Test
        @DisplayName("self-referencing FK to non-existent parent is rejected")
        void selfReferencingFkInvalid() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO answer (post_id, content, user_id, question_id) VALUES (999999, 'Bad child', ?, ?)",
                    userId, questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO answer (content, user_id, question_id) VALUES ('c', 999999, ?)",
                    questionId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to question is enforced")
        void foreignKeyToQuestion() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO answer (content, user_id, question_id) VALUES ('c', ?, 999999)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all columns present in the answer table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("ANSWER");
            assertThat(columns).containsExactlyInAnyOrder(
                "answer_id", "post_id", "content", "created_at", "updated_at",
                "deleted_at", "is_accepted", "like_count", "user_id", "question_id"
            );
        }
    }

    // =========================================================
    // answer_like table tests
    // =========================================================
    @Nested
    @DisplayName("answer_like table")
    class AnswerLikeTableTests {

        private long answerId;

        @BeforeEach
        void setUpAnswer() {
            jdbc.update(
                "INSERT INTO answer (content, user_id, question_id) VALUES ('answer for like test', ?, ?)",
                userId, questionId
            );
            answerId = jdbc.queryForObject("SELECT MAX(answer_id) FROM answer", Long.class);
        }

        @Test
        @DisplayName("answer_like table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "ANSWER_LIKE", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("valid answer like can be inserted")
        void insertValidLike() {
            jdbc.update(
                "INSERT INTO answer_like (user_id, answer_id) VALUES (?, ?)",
                userId, answerId
            );
            int count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM answer_like WHERE user_id = ? AND answer_id = ?",
                Integer.class, userId, answerId
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO answer_like (user_id, answer_id) VALUES (999999, ?)", answerId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to answer is enforced")
        void foreignKeyToAnswer() {
            assertThatThrownBy(() ->
                jdbc.update("INSERT INTO answer_like (user_id, answer_id) VALUES (?, 999999)", userId)
            ).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // =========================================================
    // comment table tests
    // =========================================================
    @Nested
    @DisplayName("comment table")
    class CommentTableTests {

        private long answerId;

        @BeforeEach
        void setUpAnswer() {
            jdbc.update(
                "INSERT INTO answer (content, user_id, question_id) VALUES ('answer for comment test', ?, ?)",
                userId, questionId
            );
            answerId = jdbc.queryForObject("SELECT MAX(answer_id) FROM answer", Long.class);
        }

        @Test
        @DisplayName("comment table exists after migration")
        void tableExists() throws Exception {
            DatabaseMetaData meta = jdbc.getDataSource().getConnection().getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "COMMENT", new String[]{"TABLE"})) {
                assertThat(rs.next()).isTrue();
            }
        }

        @Test
        @DisplayName("comment_id is auto-incremented primary key")
        void primaryKeyAutoIncrement() {
            jdbc.update(
                "INSERT INTO comment (content, answer_id, user_id) VALUES ('c1', ?, ?)",
                answerId, userId
            );
            long id1 = jdbc.queryForObject("SELECT MAX(comment_id) FROM comment", Long.class);

            jdbc.update(
                "INSERT INTO comment (content, answer_id, user_id) VALUES ('c2', ?, ?)",
                answerId, userId
            );
            long id2 = jdbc.queryForObject("SELECT MAX(comment_id) FROM comment", Long.class);

            assertThat(id2).isGreaterThan(id1);
        }

        @Test
        @DisplayName("content NOT NULL constraint is enforced")
        void contentNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO comment (content, answer_id, user_id) VALUES (NULL, ?, ?)",
                    answerId, userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("answer_id NOT NULL constraint is enforced")
        void answerIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO comment (content, answer_id, user_id) VALUES ('c', NULL, ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("user_id NOT NULL constraint is enforced")
        void userIdNotNull() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO comment (content, answer_id, user_id) VALUES ('c', ?, NULL)",
                    answerId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("created_at and updated_at default to CURRENT_TIMESTAMP")
        void timestampDefaults() {
            jdbc.update(
                "INSERT INTO comment (content, answer_id, user_id) VALUES ('ts test', ?, ?)",
                answerId, userId
            );
            long id = jdbc.queryForObject("SELECT MAX(comment_id) FROM comment", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at FROM comment WHERE comment_id = ?", id
            );
            assertThat(row.get("created_at")).isNotNull();
            assertThat(row.get("updated_at")).isNotNull();
        }

        @Test
        @DisplayName("deleted_at is nullable")
        void deletedAtNullable() {
            jdbc.update(
                "INSERT INTO comment (content, answer_id, user_id) VALUES ('nullable', ?, ?)",
                answerId, userId
            );
            long id = jdbc.queryForObject("SELECT MAX(comment_id) FROM comment", Long.class);
            Map<String, Object> row = jdbc.queryForMap(
                "SELECT deleted_at FROM comment WHERE comment_id = ?", id
            );
            assertThat(row.get("deleted_at")).isNull();
        }

        @Test
        @DisplayName("foreign key to answer is enforced")
        void foreignKeyToAnswer() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO comment (content, answer_id, user_id) VALUES ('c', 999999, ?)",
                    userId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("foreign key to users is enforced")
        void foreignKeyToUsers() {
            assertThatThrownBy(() ->
                jdbc.update(
                    "INSERT INTO comment (content, answer_id, user_id) VALUES ('c', ?, 999999)",
                    answerId
                )
            ).isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("all columns present in the comment table")
        void allColumnsPresent() throws Exception {
            List<String> columns = getColumnNames("COMMENT");
            assertThat(columns).containsExactlyInAnyOrder(
                "comment_id", "content", "created_at", "updated_at",
                "answer_id", "user_id", "deleted_at"
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