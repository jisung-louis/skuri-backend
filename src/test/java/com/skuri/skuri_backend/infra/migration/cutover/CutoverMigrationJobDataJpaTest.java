package com.skuri.skuri_backend.infra.migration.cutover;

import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftIdentityService;
import com.skuri.skuri_backend.infra.migration.FirestoreTimestampParser;
import com.skuri.skuri_backend.infra.migration.MigrationExecutionResult;
import com.skuri.skuri_backend.infra.migration.MigrationMode;
import com.skuri.skuri_backend.infra.migration.MigrationReportWriter;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({
        ObjectMapperConfig.class,
        FirestoreTimestampParser.class,
        MigrationReportWriter.class,
        MembersMigrationJob.class,
        TimetableMigrationJob.class,
        MinecraftAccountMigrationJob.class,
        CutoverMigrationJob.class,
        MinecraftIdentityService.class,
        CutoverMigrationJobDataJpaTest.TestConfig.class
})
class CutoverMigrationJobDataJpaTest {

    @Autowired
    private CutoverMigrationJob cutoverMigrationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @TempDir
    Path tempDir;

    @Test
    void apply_회원_시간표_마인크래프트를_함께_이관한다() throws Exception {
        insertLiveCourse(
                "course-live-1",
                "2026-1",
                "21747",
                "001",
                "자료구조",
                "컴퓨터공학과",
                "기념관101",
                2,
                3
        );

        Path usersFile = tempDir.resolve("users.json");
        Files.writeString(usersFile, """
                [
                  {
                    "id": "member-1",
                    "data": {
                      "uid": "member-1",
                      "email": "member1@sungkyul.ac.kr",
                      "displayName": "앱닉네임",
                      "studentId": "20260001",
                      "department": "컴퓨터공학과",
                      "photoURL": "https://example.com/profile.jpg",
                      "createdAt": {"_seconds": 1775000000, "_nanoseconds": 0},
                      "lastLogin": {"_seconds": 1775000600, "_nanoseconds": 0},
                      "lastLoginOS": "ios",
                      "currentVersion": "1.2.7",
                      "linkedAccounts": [
                        {
                          "displayName": "홍길동",
                          "email": "member1@sungkyul.ac.kr",
                          "photoURL": "https://example.com/google.jpg",
                          "provider": "google",
                          "providerId": "google-provider-id"
                        }
                      ],
                      "notificationSettings": {
                        "allNotifications": true,
                        "partyNotifications": false,
                        "noticeNotifications": true,
                        "boardLikeNotifications": false,
                        "boardCommentNotifications": true,
                        "systemNotifications": true
                      },
                      "account": {
                        "bankName": "국민",
                        "accountNumber": "123-456",
                        "accountHolder": "홍길동",
                        "hideName": true
                      },
                      "fcmTokens": ["fcm-token-1"],
                      "minecraftAccount": {
                        "accounts": [
                          {
                            "edition": "BE",
                            "linkedAt": 1775001000000,
                            "nickname": "VoicedSalt38750",
                            "storedName": "VoicedSalt38",
                            "uuid": "be:VoicedSalt38"
                          },
                          {
                            "edition": "BE",
                            "linkedAt": 1775002000000,
                            "nickname": "jixxy7806",
                            "storedName": "jixxy7806",
                            "uuid": "be:jixxy7806",
                            "whoseFriend": "VoicedSalt38750"
                          }
                        ]
                      }
                    }
                  }
                ]
                """);

        Path timetablesFile = tempDir.resolve("timetables.json");
        Files.writeString(timetablesFile, """
                [
                  {
                    "id": "timetable-firestore-1",
                    "data": {
                      "userId": "member-1",
                      "semester": "2026-1",
                      "createdAt": {"_seconds": 1775000000, "_nanoseconds": 0},
                      "updatedAt": {"_seconds": 1775000800, "_nanoseconds": 0},
                      "courses": ["firestore-course-1"]
                    }
                  }
                ]
                """);

        Path coursesFile = tempDir.resolve("courses.json");
        Files.writeString(coursesFile, """
                [
                  {
                    "id": "firestore-course-1",
                    "data": {
                      "semester": "2026-1",
                      "code": "21747",
                      "division": "001",
                      "name": "자료구조",
                      "department": "컴퓨터공학과",
                      "location": "기념관101",
                      "schedule": [
                        {"dayOfWeek": 2, "startPeriod": 3, "endPeriod": 3}
                      ]
                    }
                  }
                ]
                """);

        Path minecraftFile = tempDir.resolve("minecraft.json");
        Files.writeString(minecraftFile, """
                {
                  "players": {},
                  "BEPlayers": {
                    "VoicedSalt38": {
                      "storedName": "VoicedSalt38",
                      "addedAt": 1775001000000,
                      "lastSeenAt": 1775005000000
                    },
                    "jixxy7806": {
                      "storedName": "jixxy7806",
                      "addedAt": 1775002000000,
                      "lastSeenAt": 1775006000000
                    }
                  }
                }
                """);

        MigrationExecutionResult result = cutoverMigrationJob.execute(
                usersFile,
                coursesFile,
                timetablesFile,
                minecraftFile,
                new MigrationRunOptions(MigrationMode.APPLY, 100, true, tempDir.resolve("reports"), LocalDateTime.of(2026, 4, 3, 1, 0))
        );

        assertEquals(0, result.rejects().size());
        assertTrue(Files.exists(result.reportDirectory().resolve("course-matches.json")));
        assertTrue(Files.exists(result.reportDirectory().resolve("member-rejects.json")));

        Map<String, Object> member = jdbcTemplate.queryForMap(
                "select nickname, realname, student_id, bank_name, party_notifications, board_like_notifications, comment_notifications from members where id = ?",
                "member-1"
        );
        assertEquals("앱닉네임", member.get("nickname"));
        assertEquals("홍길동", member.get("realname"));
        assertEquals("20260001", member.get("student_id"));
        assertEquals("국민", member.get("bank_name"));
        assertEquals(false, member.get("party_notifications"));
        assertEquals(false, member.get("board_like_notifications"));
        assertEquals(true, member.get("comment_notifications"));

        Map<String, Object> linkedAccount = jdbcTemplate.queryForMap(
                "select provider, provider_display_name from linked_accounts where member_id = ?",
                "member-1"
        );
        assertEquals("GOOGLE", linkedAccount.get("provider"));
        assertEquals("홍길동", linkedAccount.get("provider_display_name"));

        Map<String, Object> fcmToken = jdbcTemplate.queryForMap(
                "select user_id, token, platform, app_version from fcm_tokens where user_id = ?",
                "member-1"
        );
        assertEquals("fcm-token-1", fcmToken.get("token"));
        assertEquals("ios", fcmToken.get("platform"));

        Map<String, Object> timetable = jdbcTemplate.queryForMap(
                "select id, user_id, semester from user_timetables where user_id = ?",
                "member-1"
        );
        assertEquals("timetable-firestore-1", timetable.get("id"));
        assertEquals("2026-1", timetable.get("semester"));
        Integer timetableCourseCount = jdbcTemplate.queryForObject(
                "select count(*) from user_timetable_courses where timetable_id = ? and course_id = ?",
                Integer.class,
                "timetable-firestore-1",
                "course-live-1"
        );
        assertEquals(1, timetableCourseCount);

        List<Map<String, Object>> minecraftRows = jdbcTemplate.queryForList(
                "select account_role, game_name, stored_name, parent_account_id from minecraft_accounts where owner_member_id = ? order by account_role asc, game_name asc",
                "member-1"
        );
        assertEquals(2, minecraftRows.size());
        assertEquals("FRIEND", minecraftRows.get(0).get("account_role"));
        assertEquals("SELF", minecraftRows.get(1).get("account_role"));
        assertTrue(minecraftRows.get(0).get("parent_account_id") != null);
    }

    @Test
    void dryRun_알수없는_userId_시간표는_reject로남기고_DB를변경하지않는다() throws Exception {
        Path usersFile = tempDir.resolve("users-empty.json");
        Files.writeString(usersFile, "[]");
        Path timetablesFile = tempDir.resolve("timetables-unknown-user.json");
        Files.writeString(timetablesFile, """
                [
                  {
                    "id": "unknown-timetable",
                    "data": {
                      "userId": "missing-user",
                      "semester": "2026-1",
                      "createdAt": {"_seconds": 1775000000, "_nanoseconds": 0},
                      "courses": []
                    }
                  }
                ]
                """);
        Path coursesFile = tempDir.resolve("courses-empty.json");
        Files.writeString(coursesFile, "[]");
        Path minecraftFile = tempDir.resolve("minecraft-empty.json");
        Files.writeString(minecraftFile, "{\"players\":{},\"BEPlayers\":{}}");

        MigrationExecutionResult result = cutoverMigrationJob.execute(
                usersFile,
                coursesFile,
                timetablesFile,
                minecraftFile,
                new MigrationRunOptions(MigrationMode.DRY_RUN, 100, false, tempDir.resolve("dry-run-reports"), LocalDateTime.of(2026, 4, 3, 1, 30))
        );

        assertEquals(1, result.rejects().size());
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from user_timetables", Integer.class));
        assertTrue(Files.exists(result.reportDirectory().resolve("timetable-rejects.json")));
    }

    private void insertLiveCourse(
            String id,
            String semester,
            String code,
            String division,
            String name,
            String department,
            String location,
            int dayOfWeek,
            int startPeriod
    ) {
        entityManager.createNativeQuery("""
                insert into courses (
                    id, grade, category, code, division, name, credits, professor, location, note, is_online,
                    semester, department, created_at, updated_at
                ) values (
                    :id, :grade, :category, :code, :division, :name, :credits, :professor, :location, :note, :online,
                    :semester, :department, :createdAt, :updatedAt
                )
                """)
                .setParameter("id", id)
                .setParameter("grade", 2)
                .setParameter("category", "전공선택")
                .setParameter("code", code)
                .setParameter("division", division)
                .setParameter("name", name)
                .setParameter("credits", 3)
                .setParameter("professor", "교수님")
                .setParameter("location", location)
                .setParameter("note", "")
                .setParameter("online", false)
                .setParameter("semester", semester)
                .setParameter("department", department)
                .setParameter("createdAt", LocalDateTime.of(2026, 3, 1, 9, 0))
                .setParameter("updatedAt", LocalDateTime.of(2026, 3, 1, 9, 0))
                .executeUpdate();
        entityManager.createNativeQuery("""
                insert into course_schedules (course_id, day_of_week, start_period, end_period)
                values (:courseId, :dayOfWeek, :startPeriod, :endPeriod)
                """)
                .setParameter("courseId", id)
                .setParameter("dayOfWeek", dayOfWeek)
                .setParameter("startPeriod", startPeriod)
                .setParameter("endPeriod", startPeriod)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    static class TestConfig {

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        MinecraftBridgeProperties minecraftBridgeProperties() {
            return new MinecraftBridgeProperties(
                    "secret",
                    "public:game:minecraft",
                    "skuri",
                    "8667ba71b85a4004af54457a9734eed7",
                    86_400L
            );
        }
    }
}
