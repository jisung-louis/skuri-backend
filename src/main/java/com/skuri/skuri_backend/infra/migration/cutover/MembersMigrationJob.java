package com.skuri.skuri_backend.infra.migration.cutover;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.member.entity.LinkedAccountProvider;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.migration.FirestoreTimestampParser;
import com.skuri.skuri_backend.infra.migration.MigrationMode;
import com.skuri.skuri_backend.infra.migration.MigrationReject;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MembersMigrationJob {

    private static final String INSERT_MEMBER_SQL = """
            insert into members (
                id, email, nickname, student_id, department, photo_url, realname, is_admin, status,
                bank_name, account_number, account_holder, hide_name,
                all_notifications, party_notifications, notice_notifications, board_like_notifications,
                comment_notifications, bookmarked_post_comment_notifications, system_notifications,
                academic_schedule_notifications, academic_schedule_day_before_enabled,
                academic_schedule_all_events_enabled, notice_notifications_detail,
                joined_at, last_login, withdrawn_at, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_MEMBER_SQL = """
            update members
            set email = ?,
                nickname = ?,
                student_id = ?,
                department = ?,
                photo_url = ?,
                realname = ?,
                is_admin = ?,
                status = ?,
                bank_name = ?,
                account_number = ?,
                account_holder = ?,
                hide_name = ?,
                all_notifications = ?,
                party_notifications = ?,
                notice_notifications = ?,
                board_like_notifications = ?,
                comment_notifications = ?,
                bookmarked_post_comment_notifications = ?,
                system_notifications = ?,
                academic_schedule_notifications = ?,
                academic_schedule_day_before_enabled = ?,
                academic_schedule_all_events_enabled = ?,
                notice_notifications_detail = ?,
                joined_at = ?,
                last_login = ?,
                withdrawn_at = ?,
                updated_at = ?
            where id = ?
            """;

    private static final String INSERT_LINKED_ACCOUNT_SQL = """
            insert into linked_accounts (
                member_id, provider, provider_id, email, provider_display_name, photo_url, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_FCM_TOKEN_SQL = """
            insert into fcm_tokens (
                user_id, token, platform, app_version, created_at, last_used_at
            ) values (?, ?, ?, ?, ?, ?)
            """;

    private final MemberRepository memberRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final FirestoreTimestampParser timestampParser;

    public MembersMigrationJob(
            MemberRepository memberRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            FirestoreTimestampParser timestampParser
    ) {
        this.memberRepository = memberRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.timestampParser = timestampParser;
    }

    @Transactional
    public CutoverDomainResult execute(Path usersFile, MigrationRunOptions options) {
        List<UserExportItem> items = readUsers(usersFile);
        List<MigrationReject> rejects = new ArrayList<>();
        Set<String> seenMemberIds = new HashSet<>();
        Set<String> seenFcmTokens = new HashSet<>();

        List<MemberRow> memberRows = new ArrayList<>();
        List<LinkedAccountRow> linkedAccountRows = new ArrayList<>();
        List<FcmTokenRow> fcmTokenRows = new ArrayList<>();

        for (UserExportItem item : items) {
            try {
                UserMappedRows mappedRows = mapUser(item, options.startedAt(), seenMemberIds, seenFcmTokens);
                memberRows.add(mappedRows.memberRow());
                linkedAccountRows.addAll(mappedRows.linkedAccountRows());
                fcmTokenRows.addAll(mappedRows.fcmTokenRows());
            } catch (IllegalArgumentException e) {
                rejects.add(new MigrationReject(item.id(), e.getMessage(), Map.of("domain", "members")));
            }
        }

        Set<String> existingMemberIds = findExistingMemberIds(memberRows);
        List<MemberRow> inserts = memberRows.stream()
                .filter(row -> !existingMemberIds.contains(row.id()))
                .toList();
        List<MemberRow> updates = memberRows.stream()
                .filter(row -> existingMemberIds.contains(row.id()))
                .toList();

        if (options.mode() == MigrationMode.APPLY && !memberRows.isEmpty()) {
            jdbcTemplate.batchUpdate(INSERT_MEMBER_SQL, new MemberInsertSetter(inserts));
            jdbcTemplate.batchUpdate(UPDATE_MEMBER_SQL, new MemberUpdateSetter(updates));

            List<String> memberIds = memberRows.stream().map(MemberRow::id).toList();
            deleteChildRows("linked_accounts", "member_id", memberIds, options.batchSize());
            deleteChildRows("fcm_tokens", "user_id", memberIds, options.batchSize());

            if (!linkedAccountRows.isEmpty()) {
                jdbcTemplate.batchUpdate(INSERT_LINKED_ACCOUNT_SQL, new LinkedAccountInsertSetter(linkedAccountRows));
            }
            if (!fcmTokenRows.isEmpty()) {
                jdbcTemplate.batchUpdate(INSERT_FCM_TOKEN_SQL, new FcmTokenInsertSetter(fcmTokenRows));
            }
        }

        return new CutoverDomainResult(
                "members",
                Map.of(
                        "users_scanned", (long) items.size(),
                        "members_inserted", (long) inserts.size(),
                        "members_updated", (long) updates.size(),
                        "linked_accounts_inserted", (long) linkedAccountRows.size(),
                        "fcm_tokens_inserted", (long) fcmTokenRows.size()
                ),
                List.copyOf(rejects),
                Map.of()
        );
    }

    private List<UserExportItem> readUsers(Path usersFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(usersFile), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("users export를 읽는 중 실패했습니다: " + usersFile, e);
        }
    }

    private UserMappedRows mapUser(
            UserExportItem item,
            LocalDateTime cutoverStartedAt,
            Set<String> seenMemberIds,
            Set<String> seenFcmTokens
    ) {
        UserExportData data = item.data();
        if (data == null) {
            throw new IllegalArgumentException("data가 비어 있습니다.");
        }

        String memberId = firstNonBlank(item.id(), data.uid());
        if (!StringUtils.hasText(memberId)) {
            throw new IllegalArgumentException("member id가 비어 있습니다.");
        }
        if (!seenMemberIds.add(memberId)) {
            throw new IllegalArgumentException("중복 member id입니다: " + memberId);
        }

        String email = trimToNull(data.email());
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("email이 비어 있습니다.");
        }

        LocalDateTime joinedAt = coalesce(
                timestampParser.toLocalDateTime(data.createdAt()),
                timestampParser.toLocalDateTime(data.joinedAt()),
                cutoverStartedAt
        );
        LocalDateTime lastLogin = coalesce(
                timestampParser.toLocalDateTime(data.lastLogin()),
                timestampParser.toLocalDateTime(data.lastActiveAt()),
                joinedAt
        );
        LocalDateTime updatedAt = coalesce(
                timestampParser.toLocalDateTime(data.lastActiveAt()),
                timestampParser.toLocalDateTime(data.lastLogin()),
                joinedAt
        );

        NotificationColumns notificationColumns = NotificationColumns.from(data.notificationSettings(), objectMapper);
        BankAccountColumns bankAccountColumns = BankAccountColumns.from(data.account(), data.accountInfo());

        MemberRow memberRow = new MemberRow(
                memberId,
                email,
                trimToNull(data.displayName()),
                trimToNull(data.studentId()),
                trimToNull(data.department()),
                trimToNull(data.photoURL()),
                firstNonBlank(linkedAccountDisplayName(data.linkedAccounts()), trimToNull(data.realname())),
                false,
                "ACTIVE",
                bankAccountColumns.bankName(),
                bankAccountColumns.accountNumber(),
                bankAccountColumns.accountHolder(),
                bankAccountColumns.hideName(),
                notificationColumns.allNotifications(),
                notificationColumns.partyNotifications(),
                notificationColumns.noticeNotifications(),
                notificationColumns.boardLikeNotifications(),
                notificationColumns.commentNotifications(),
                notificationColumns.bookmarkedPostCommentNotifications(),
                notificationColumns.systemNotifications(),
                notificationColumns.academicScheduleNotifications(),
                notificationColumns.academicScheduleDayBeforeEnabled(),
                notificationColumns.academicScheduleAllEventsEnabled(),
                notificationColumns.noticeNotificationsDetailJson(),
                joinedAt,
                lastLogin,
                null,
                joinedAt,
                updatedAt
        );

        List<LinkedAccountRow> linkedAccountRows = mapLinkedAccounts(memberId, data.linkedAccounts(), joinedAt, updatedAt);
        List<FcmTokenRow> fcmTokenRows = mapFcmTokens(
                memberId,
                data.fcmTokens(),
                trimToNull(data.lastLoginOS()),
                trimToNull(data.currentVersion()),
                lastUsedAt(data.lastLogin(), data.lastActiveAt()),
                joinedAt,
                cutoverStartedAt,
                seenFcmTokens
        );

        return new UserMappedRows(memberRow, linkedAccountRows, fcmTokenRows);
    }

    private List<LinkedAccountRow> mapLinkedAccounts(
            String memberId,
            List<LinkedAccountExportData> linkedAccounts,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        if (linkedAccounts == null || linkedAccounts.isEmpty()) {
            return List.of();
        }
        Map<String, LinkedAccountRow> deduplicated = new LinkedHashMap<>();
        for (LinkedAccountExportData linkedAccount : linkedAccounts) {
            String provider = resolveProvider(linkedAccount.provider());
            if (deduplicated.containsKey(provider)) {
                throw new IllegalArgumentException("같은 provider의 linkedAccount가 중복됩니다. memberId=" + memberId + ", provider=" + provider);
            }
            deduplicated.put(provider, new LinkedAccountRow(
                    memberId,
                    provider,
                    trimToNull(linkedAccount.providerId()),
                    trimToNull(linkedAccount.email()),
                    trimToNull(linkedAccount.displayName()),
                    trimToNull(linkedAccount.photoURL()),
                    createdAt,
                    updatedAt
            ));
        }
        return List.copyOf(deduplicated.values());
    }

    private List<FcmTokenRow> mapFcmTokens(
            String memberId,
            List<String> fcmTokens,
            String platform,
            String appVersion,
            LocalDateTime lastUsedAt,
            LocalDateTime joinedAt,
            LocalDateTime cutoverStartedAt,
            Set<String> seenFcmTokens
    ) {
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            return List.of();
        }
        List<FcmTokenRow> rows = new ArrayList<>();
        for (String token : fcmTokens) {
            String normalizedToken = trimToNull(token);
            if (!StringUtils.hasText(normalizedToken)) {
                continue;
            }
            if (!seenFcmTokens.add(normalizedToken)) {
                throw new IllegalArgumentException("중복 FCM token이 존재합니다: " + normalizedToken);
            }
            rows.add(new FcmTokenRow(
                    memberId,
                    normalizedToken,
                    StringUtils.hasText(platform) ? platform : "unknown",
                    appVersion,
                    coalesce(lastUsedAt, joinedAt, cutoverStartedAt),
                    lastUsedAt
            ));
        }
        return List.copyOf(rows);
    }

    private Set<String> findExistingMemberIds(List<MemberRow> memberRows) {
        if (memberRows.isEmpty()) {
            return Set.of();
        }
        Set<String> existing = new HashSet<>();
        for (Member member : memberRepository.findAllById(memberRows.stream().map(MemberRow::id).toList())) {
            existing.add(member.getId());
        }
        return existing;
    }

    private void deleteChildRows(String tableName, String columnName, List<String> ids, int batchSize) {
        for (List<String> chunk : CutoverBatchUtils.partition(ids, batchSize)) {
            String placeholders = chunk.stream().map(id -> "?").collect(Collectors.joining(", "));
            jdbcTemplate.update("delete from " + tableName + " where " + columnName + " in (" + placeholders + ")", chunk.toArray());
        }
    }

    private LocalDateTime lastUsedAt(JsonNode lastLogin, JsonNode lastActiveAt) {
        return coalesce(
                timestampParser.toLocalDateTime(lastLogin),
                timestampParser.toLocalDateTime(lastActiveAt)
        );
    }

    private String linkedAccountDisplayName(List<LinkedAccountExportData> linkedAccounts) {
        if (linkedAccounts == null) {
            return null;
        }
        for (LinkedAccountExportData linkedAccount : linkedAccounts) {
            String displayName = trimToNull(linkedAccount.displayName());
            if (StringUtils.hasText(displayName)) {
                return displayName;
            }
        }
        return null;
    }

    private String resolveProvider(String signInProvider) {
        String trimmed = trimToNull(signInProvider);
        if (!StringUtils.hasText(trimmed)) {
            return LinkedAccountProvider.UNKNOWN.name();
        }
        return switch (trimmed) {
            case "google", "google.com" -> LinkedAccountProvider.GOOGLE.name();
            case "password" -> LinkedAccountProvider.PASSWORD.name();
            default -> LinkedAccountProvider.UNKNOWN.name();
        };
    }

    private static LocalDateTime coalesce(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (StringUtils.hasText(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private final class MemberInsertSetter implements BatchPreparedStatementSetter {
        private final List<MemberRow> rows;

        private MemberInsertSetter(List<MemberRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            MemberRow row = rows.get(i);
            ps.setString(1, row.id());
            ps.setString(2, row.email());
            ps.setString(3, row.nickname());
            ps.setString(4, row.studentId());
            ps.setString(5, row.department());
            ps.setString(6, row.photoUrl());
            ps.setString(7, row.realname());
            ps.setBoolean(8, row.isAdmin());
            ps.setString(9, row.status());
            ps.setString(10, row.bankName());
            ps.setString(11, row.accountNumber());
            ps.setString(12, row.accountHolder());
            if (row.hideName() == null) {
                ps.setObject(13, null);
            } else {
                ps.setBoolean(13, row.hideName());
            }
            ps.setBoolean(14, row.allNotifications());
            ps.setBoolean(15, row.partyNotifications());
            ps.setBoolean(16, row.noticeNotifications());
            ps.setBoolean(17, row.boardLikeNotifications());
            ps.setBoolean(18, row.commentNotifications());
            ps.setBoolean(19, row.bookmarkedPostCommentNotifications());
            ps.setBoolean(20, row.systemNotifications());
            ps.setBoolean(21, row.academicScheduleNotifications());
            ps.setBoolean(22, row.academicScheduleDayBeforeEnabled());
            ps.setBoolean(23, row.academicScheduleAllEventsEnabled());
            ps.setString(24, row.noticeNotificationsDetailJson());
            ps.setTimestamp(25, timestamp(row.joinedAt()));
            ps.setTimestamp(26, timestamp(row.lastLogin()));
            ps.setTimestamp(27, timestamp(row.withdrawnAt()));
            ps.setTimestamp(28, timestamp(row.createdAt()));
            ps.setTimestamp(29, timestamp(row.updatedAt()));
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private final class MemberUpdateSetter implements BatchPreparedStatementSetter {
        private final List<MemberRow> rows;

        private MemberUpdateSetter(List<MemberRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            MemberRow row = rows.get(i);
            ps.setString(1, row.email());
            ps.setString(2, row.nickname());
            ps.setString(3, row.studentId());
            ps.setString(4, row.department());
            ps.setString(5, row.photoUrl());
            ps.setString(6, row.realname());
            ps.setBoolean(7, row.isAdmin());
            ps.setString(8, row.status());
            ps.setString(9, row.bankName());
            ps.setString(10, row.accountNumber());
            ps.setString(11, row.accountHolder());
            if (row.hideName() == null) {
                ps.setObject(12, null);
            } else {
                ps.setBoolean(12, row.hideName());
            }
            ps.setBoolean(13, row.allNotifications());
            ps.setBoolean(14, row.partyNotifications());
            ps.setBoolean(15, row.noticeNotifications());
            ps.setBoolean(16, row.boardLikeNotifications());
            ps.setBoolean(17, row.commentNotifications());
            ps.setBoolean(18, row.bookmarkedPostCommentNotifications());
            ps.setBoolean(19, row.systemNotifications());
            ps.setBoolean(20, row.academicScheduleNotifications());
            ps.setBoolean(21, row.academicScheduleDayBeforeEnabled());
            ps.setBoolean(22, row.academicScheduleAllEventsEnabled());
            ps.setString(23, row.noticeNotificationsDetailJson());
            ps.setTimestamp(24, timestamp(row.joinedAt()));
            ps.setTimestamp(25, timestamp(row.lastLogin()));
            ps.setTimestamp(26, timestamp(row.withdrawnAt()));
            ps.setTimestamp(27, timestamp(row.updatedAt()));
            ps.setString(28, row.id());
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private final class LinkedAccountInsertSetter implements BatchPreparedStatementSetter {
        private final List<LinkedAccountRow> rows;

        private LinkedAccountInsertSetter(List<LinkedAccountRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            LinkedAccountRow row = rows.get(i);
            ps.setString(1, row.memberId());
            ps.setString(2, row.provider());
            ps.setString(3, row.providerId());
            ps.setString(4, row.email());
            ps.setString(5, row.providerDisplayName());
            ps.setString(6, row.photoUrl());
            ps.setTimestamp(7, timestamp(row.createdAt()));
            ps.setTimestamp(8, timestamp(row.updatedAt()));
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private final class FcmTokenInsertSetter implements BatchPreparedStatementSetter {
        private final List<FcmTokenRow> rows;

        private FcmTokenInsertSetter(List<FcmTokenRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            FcmTokenRow row = rows.get(i);
            ps.setString(1, row.userId());
            ps.setString(2, row.token());
            ps.setString(3, row.platform());
            ps.setString(4, row.appVersion());
            ps.setTimestamp(5, timestamp(row.createdAt()));
            ps.setTimestamp(6, timestamp(row.lastUsedAt()));
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private record UserMappedRows(
            MemberRow memberRow,
            List<LinkedAccountRow> linkedAccountRows,
            List<FcmTokenRow> fcmTokenRows
    ) {
    }

    private record MemberRow(
            String id,
            String email,
            String nickname,
            String studentId,
            String department,
            String photoUrl,
            String realname,
            boolean isAdmin,
            String status,
            String bankName,
            String accountNumber,
            String accountHolder,
            Boolean hideName,
            boolean allNotifications,
            boolean partyNotifications,
            boolean noticeNotifications,
            boolean boardLikeNotifications,
            boolean commentNotifications,
            boolean bookmarkedPostCommentNotifications,
            boolean systemNotifications,
            boolean academicScheduleNotifications,
            boolean academicScheduleDayBeforeEnabled,
            boolean academicScheduleAllEventsEnabled,
            String noticeNotificationsDetailJson,
            LocalDateTime joinedAt,
            LocalDateTime lastLogin,
            LocalDateTime withdrawnAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    private record LinkedAccountRow(
            String memberId,
            String provider,
            String providerId,
            String email,
            String providerDisplayName,
            String photoUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    private record FcmTokenRow(
            String userId,
            String token,
            String platform,
            String appVersion,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt
    ) {
    }

    private record NotificationColumns(
            boolean allNotifications,
            boolean partyNotifications,
            boolean noticeNotifications,
            boolean boardLikeNotifications,
            boolean commentNotifications,
            boolean bookmarkedPostCommentNotifications,
            boolean systemNotifications,
            boolean academicScheduleNotifications,
            boolean academicScheduleDayBeforeEnabled,
            boolean academicScheduleAllEventsEnabled,
            String noticeNotificationsDetailJson
    ) {
        private static NotificationColumns from(NotificationSettingsExportData source, ObjectMapper objectMapper) {
            Map<String, Boolean> defaults = new LinkedHashMap<>();
            defaults.put("news", Boolean.TRUE);
            defaults.put("academy", Boolean.TRUE);
            defaults.put("scholarship", Boolean.TRUE);

            boolean allNotifications = source == null || !Boolean.FALSE.equals(source.allNotifications());
            boolean partyNotifications = source == null || !Boolean.FALSE.equals(source.partyNotifications());
            boolean noticeNotifications = source == null || !Boolean.FALSE.equals(source.noticeNotifications());
            boolean boardLikeNotifications = source == null || !Boolean.FALSE.equals(source.boardLikeNotifications());
            boolean commentNotifications = source == null || !Boolean.FALSE.equals(source.boardCommentNotifications());
            boolean bookmarkedPostCommentNotifications = true;
            boolean systemNotifications = source == null || !Boolean.FALSE.equals(source.systemNotifications());
            boolean academicScheduleNotifications = true;
            boolean academicScheduleDayBeforeEnabled = true;
            boolean academicScheduleAllEventsEnabled = false;

            try {
                return new NotificationColumns(
                        allNotifications,
                        partyNotifications,
                        noticeNotifications,
                        boardLikeNotifications,
                        commentNotifications,
                        bookmarkedPostCommentNotifications,
                        systemNotifications,
                        academicScheduleNotifications,
                        academicScheduleDayBeforeEnabled,
                        academicScheduleAllEventsEnabled,
                        objectMapper.writeValueAsString(defaults)
                );
            } catch (IOException e) {
                throw new IllegalStateException("noticeNotificationsDetail JSON 직렬화에 실패했습니다.", e);
            }
        }
    }

    private record BankAccountColumns(
            String bankName,
            String accountNumber,
            String accountHolder,
            Boolean hideName
    ) {
        private static BankAccountColumns from(BankAccountExportData account, BankAccountExportData accountInfo) {
            BankAccountExportData source = account != null ? account : accountInfo;
            if (source == null) {
                return new BankAccountColumns(null, null, null, null);
            }
            return new BankAccountColumns(
                    normalize(source.bankName()),
                    normalize(source.accountNumber()),
                    normalize(source.accountHolder()),
                    source.hideName() == null ? Boolean.FALSE : source.hideName()
            );
        }

        private static String normalize(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserExportItem(
            String id,
            UserExportData data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserExportData(
            String uid,
            String email,
            String displayName,
            String studentId,
            String department,
            String photoURL,
            String realname,
            JsonNode createdAt,
            JsonNode joinedAt,
            JsonNode lastLogin,
            JsonNode lastActiveAt,
            String lastLoginOS,
            String currentVersion,
            List<String> fcmTokens,
            List<LinkedAccountExportData> linkedAccounts,
            NotificationSettingsExportData notificationSettings,
            BankAccountExportData account,
            BankAccountExportData accountInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LinkedAccountExportData(
            String displayName,
            String email,
            String photoURL,
            String provider,
            String providerId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NotificationSettingsExportData(
            Boolean allNotifications,
            Boolean partyNotifications,
            Boolean noticeNotifications,
            Boolean boardLikeNotifications,
            Boolean boardCommentNotifications,
            Boolean systemNotifications,
            Boolean marketingNotifications
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BankAccountExportData(
            String bankName,
            String accountNumber,
            String accountHolder,
            Boolean hideName
    ) {
    }
}
