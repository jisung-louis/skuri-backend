package com.skuri.skuri_backend.infra.migration.cutover;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftIdentityService;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MinecraftAccountMigrationJob {

    private static final String INSERT_SQL = """
            insert into minecraft_accounts (
                id, owner_member_id, parent_account_id, account_role, edition, game_name, stored_name,
                normalized_key, avatar_uuid, last_seen_at, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;
    private final FirestoreTimestampParser timestampParser;
    private final MinecraftIdentityService minecraftIdentityService;
    private final JdbcTemplate jdbcTemplate;

    public MinecraftAccountMigrationJob(
            ObjectMapper objectMapper,
            FirestoreTimestampParser timestampParser,
            MinecraftIdentityService minecraftIdentityService,
            JdbcTemplate jdbcTemplate
    ) {
        this.objectMapper = objectMapper;
        this.timestampParser = timestampParser;
        this.minecraftIdentityService = minecraftIdentityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CutoverDomainResult execute(
            Path usersFile,
            Path minecraftFile,
            MigrationRunOptions options
    ) {
        List<UserMinecraftExportItem> users = readUsers(usersFile);
        MinecraftWhitelistExport whitelistExport = readWhitelist(minecraftFile);

        Map<String, WhitelistEntry> whitelistByKey = buildWhitelistByKey(whitelistExport);

        List<MigrationReject> rejects = new ArrayList<>();
        Map<String, List<MinecraftAccountRow>> rowsByOwner = new LinkedHashMap<>();
        Map<String, String> sourceOwnerByNormalizedKey = new LinkedHashMap<>();

        for (UserMinecraftExportItem user : users) {
            UserMinecraftExportData data = user.data();
            List<EmbeddedMinecraftAccountExport> accounts = data == null || data.minecraftAccount() == null
                    ? List.of()
                    : data.minecraftAccount().accounts();
            if (accounts == null || accounts.isEmpty()) {
                rowsByOwner.put(user.id(), List.of());
                continue;
            }

            try {
                List<MinecraftAccountRow> ownerRows = mapOwnerAccounts(user.id(), accounts, whitelistByKey, options.startedAt(), sourceOwnerByNormalizedKey);
                rowsByOwner.put(user.id(), ownerRows);
            } catch (IllegalArgumentException e) {
                rejects.add(new MigrationReject(user.id(), e.getMessage(), Map.of("domain", "minecraft")));
            }
        }

        Set<String> embeddedKeys = rowsByOwner.values().stream()
                .flatMap(Collection::stream)
                .map(MinecraftAccountRow::normalizedKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> whitelistKeys = new LinkedHashSet<>(whitelistByKey.keySet());
        Set<String> onlyWhitelist = new LinkedHashSet<>(whitelistKeys);
        onlyWhitelist.removeAll(embeddedKeys);
        Set<String> onlyEmbedded = new LinkedHashSet<>(embeddedKeys);
        onlyEmbedded.removeAll(whitelistKeys);
        if (!onlyWhitelist.isEmpty() || !onlyEmbedded.isEmpty()) {
            rejects.add(new MigrationReject(
                    "minecraft-source-mismatch",
                    "users export와 minecraft whitelist export의 계정 목록이 일치하지 않습니다.",
                    Map.of(
                            "domain", "minecraft",
                            "onlyWhitelist", List.copyOf(onlyWhitelist),
                            "onlyEmbedded", List.copyOf(onlyEmbedded)
                    )
            ));
        }

        List<MinecraftAccountRow> allRows = rowsByOwner.values().stream()
                .flatMap(Collection::stream)
                .sorted(Comparator
                        .comparing(MinecraftAccountRow::ownerMemberId)
                        .thenComparing(row -> "SELF".equals(row.accountRole()) ? 0 : 1)
                        .thenComparing(MinecraftAccountRow::normalizedKey))
                .toList();

        if (options.mode() == MigrationMode.APPLY) {
            deleteExistingRows(users.stream().map(UserMinecraftExportItem::id).toList(), options.batchSize());
            if (!allRows.isEmpty()) {
                jdbcTemplate.batchUpdate(INSERT_SQL, new MinecraftAccountInsertSetter(allRows));
            }
        }

        List<Map<String, Object>> sourceAlignment = List.of(Map.of(
                "embeddedCount", embeddedKeys.size(),
                "whitelistCount", whitelistKeys.size(),
                "onlyWhitelist", List.copyOf(onlyWhitelist),
                "onlyEmbedded", List.copyOf(onlyEmbedded)
        ));

        return new CutoverDomainResult(
                "minecraft",
                Map.of(
                        "minecraft_users_scanned", (long) users.size(),
                        "minecraft_accounts_inserted", (long) allRows.size()
                ),
                List.copyOf(rejects),
                Map.of("minecraft-source-alignment.json", sourceAlignment)
        );
    }

    private List<UserMinecraftExportItem> readUsers(Path usersFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(usersFile), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("users export를 읽는 중 실패했습니다: " + usersFile, e);
        }
    }

    private MinecraftWhitelistExport readWhitelist(Path minecraftFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(minecraftFile), MinecraftWhitelistExport.class);
        } catch (IOException e) {
            throw new IllegalStateException("minecraft whitelist export를 읽는 중 실패했습니다: " + minecraftFile, e);
        }
    }

    private Map<String, WhitelistEntry> buildWhitelistByKey(MinecraftWhitelistExport whitelistExport) {
        List<WhitelistEntry> entries = new ArrayList<>();
        if (whitelistExport.players() != null) {
            for (Map.Entry<String, WhitelistValue> entry : whitelistExport.players().entrySet()) {
                String normalizedKey = minecraftIdentityService.normalizeJavaUuid(entry.getKey());
                entries.add(new WhitelistEntry(
                        normalizedKey,
                        timestampParser.toLocalDateTime(entry.getValue().addedAt()),
                        timestampParser.toInstant(entry.getValue().lastSeenAt())
                ));
            }
        }
        if (whitelistExport.BEPlayers() != null) {
            for (Map.Entry<String, WhitelistValue> entry : whitelistExport.BEPlayers().entrySet()) {
                String storedName = firstNonBlank(entry.getValue().storedName(), entry.getKey());
                String normalizedKey = minecraftIdentityService.normalizeAccountKey(MinecraftEdition.BEDROCK, storedName, storedName);
                entries.add(new WhitelistEntry(
                        normalizedKey,
                        timestampParser.toLocalDateTime(entry.getValue().addedAt()),
                        timestampParser.toInstant(entry.getValue().lastSeenAt())
                ));
            }
        }
        return entries.stream()
                .collect(Collectors.toMap(WhitelistEntry::normalizedKey, entry -> entry, (left, right) -> left, LinkedHashMap::new));
    }

    private List<MinecraftAccountRow> mapOwnerAccounts(
            String ownerMemberId,
            List<EmbeddedMinecraftAccountExport> accounts,
            Map<String, WhitelistEntry> whitelistByKey,
            LocalDateTime cutoverStartedAt,
            Map<String, String> sourceOwnerByNormalizedKey
    ) {
        Map<String, CanonicalMinecraftSource> canonicalSources = new LinkedHashMap<>();
        for (EmbeddedMinecraftAccountExport account : accounts) {
            CanonicalMinecraftSource source = canonicalize(ownerMemberId, account, whitelistByKey, cutoverStartedAt);
            String existingOwner = sourceOwnerByNormalizedKey.putIfAbsent(source.normalizedKey(), ownerMemberId);
            if (existingOwner != null && !existingOwner.equals(ownerMemberId)) {
                throw new IllegalArgumentException("여러 owner가 같은 minecraft normalizedKey를 가집니다: " + source.normalizedKey());
            }
            if (canonicalSources.putIfAbsent(source.normalizedKey(), source) != null) {
                throw new IllegalArgumentException("같은 owner 안에 중복 minecraft account가 있습니다: " + source.normalizedKey());
            }
        }

        Map<String, CanonicalMinecraftSource> selfByReferenceKey = new LinkedHashMap<>();
        List<CanonicalMinecraftSource> selfSources = canonicalSources.values().stream()
                .filter(source -> source.accountRole().equals("SELF"))
                .toList();
        for (CanonicalMinecraftSource selfSource : selfSources) {
            registerReference(selfByReferenceKey, selfSource.nickname(), selfSource);
            registerReference(selfByReferenceKey, selfSource.storedName(), selfSource);
            registerReference(selfByReferenceKey, selfSource.uuid(), selfSource);
            if (selfSource.uuid() != null && selfSource.uuid().startsWith("be:")) {
                registerReference(selfByReferenceKey, selfSource.uuid().substring(3), selfSource);
            }
        }

        Map<String, String> deterministicIds = canonicalSources.values().stream()
                .collect(Collectors.toMap(
                        CanonicalMinecraftSource::normalizedKey,
                        source -> deterministicId(ownerMemberId, source.normalizedKey())
                ));

        List<MinecraftAccountRow> rows = new ArrayList<>();
        for (CanonicalMinecraftSource source : canonicalSources.values()) {
            String parentAccountId = null;
            if ("FRIEND".equals(source.accountRole())) {
                CanonicalMinecraftSource parent = resolveParentSelf(source, selfByReferenceKey, selfSources);
                if (parent == null) {
                    throw new IllegalArgumentException(
                            "friend minecraft account의 parent self를 찾을 수 없습니다. ownerMemberId="
                                    + ownerMemberId + ", whoseFriend=" + source.whoseFriend()
                        );
                }
                parentAccountId = deterministicIds.get(parent.normalizedKey());
            }

            rows.add(new MinecraftAccountRow(
                    deterministicIds.get(source.normalizedKey()),
                    ownerMemberId,
                    parentAccountId,
                    source.accountRole(),
                    source.edition(),
                    source.nickname(),
                    source.storedName(),
                    source.normalizedKey(),
                    source.avatarUuid(),
                    source.lastSeenAt(),
                    source.createdAt(),
                    source.updatedAt()
            ));
        }
        return rows.stream()
                .sorted(Comparator.comparing(MinecraftAccountRow::accountRole).thenComparing(MinecraftAccountRow::normalizedKey))
                .toList();
    }

    private CanonicalMinecraftSource canonicalize(
            String ownerMemberId,
            EmbeddedMinecraftAccountExport account,
            Map<String, WhitelistEntry> whitelistByKey,
            LocalDateTime cutoverStartedAt
    ) {
        String edition = resolveEdition(account.edition());
        String nickname = requireText(account.nickname(), "minecraft nickname");
        String storedName = null;
        String normalizedKey;
        String avatarUuid;
        if (edition.equals(MinecraftEdition.JAVA.name())) {
            String rawUuid = requireText(account.uuid(), "minecraft uuid");
            normalizedKey = minecraftIdentityService.normalizeJavaUuid(rawUuid);
            avatarUuid = minecraftIdentityService.resolveAvatarUuid(MinecraftEdition.JAVA, normalizedKey);
        } else {
            String rawStoredName = firstNonBlank(account.storedName(), stripBedrockPrefix(account.uuid()), nickname);
            storedName = minecraftIdentityService.toStoredName(rawStoredName);
            normalizedKey = minecraftIdentityService.normalizeAccountKey(MinecraftEdition.BEDROCK, nickname, storedName);
            avatarUuid = minecraftIdentityService.resolveAvatarUuid(MinecraftEdition.BEDROCK, normalizedKey);
        }

        WhitelistEntry whitelistEntry = whitelistByKey.get(normalizedKey);
        LocalDateTime createdAt = coalesce(
                timestampParser.toLocalDateTime(account.linkedAt()),
                whitelistEntry == null ? null : whitelistEntry.addedAt(),
                cutoverStartedAt
        );
        Instant lastSeenAt = whitelistEntry == null ? null : whitelistEntry.lastSeenAt();
        LocalDateTime updatedAt = coalesce(
                lastSeenAt == null ? null : LocalDateTime.ofInstant(lastSeenAt, SEOUL_ZONE),
                createdAt
        );

        return new CanonicalMinecraftSource(
                ownerMemberId,
                normalizeReference(nickname),
                edition,
                nickname,
                storedName,
                trimToNull(account.uuid()),
                normalizedKey,
                avatarUuid,
                StringUtils.hasText(trimToNull(account.whoseFriend())) ? "FRIEND" : "SELF",
                normalizeReference(account.whoseFriend()),
                createdAt,
                updatedAt,
                lastSeenAt
        );
    }

    private CanonicalMinecraftSource resolveParentSelf(
            CanonicalMinecraftSource source,
            Map<String, CanonicalMinecraftSource> selfByReferenceKey,
            List<CanonicalMinecraftSource> selfSources
    ) {
        if (source.whoseFriend() == null) {
            return null;
        }
        CanonicalMinecraftSource matched = selfByReferenceKey.get(source.whoseFriend());
        if (matched != null) {
            return matched;
        }
        return selfSources.size() == 1 ? selfSources.getFirst() : null;
    }

    private void registerReference(Map<String, CanonicalMinecraftSource> selfByReferenceKey, String rawValue, CanonicalMinecraftSource source) {
        String normalized = normalizeReference(rawValue);
        if (normalized != null) {
            selfByReferenceKey.putIfAbsent(normalized, source);
        }
    }

    private void deleteExistingRows(List<String> ownerMemberIds, int batchSize) {
        for (List<String> chunk : CutoverBatchUtils.partition(ownerMemberIds, batchSize)) {
            String placeholders = chunk.stream().map(value -> "?").collect(Collectors.joining(", "));
            jdbcTemplate.update("delete from minecraft_accounts where owner_member_id in (" + placeholders + ")", chunk.toArray());
        }
    }

    private String resolveEdition(String rawEdition) {
        String trimmed = trimToNull(rawEdition);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException("minecraft edition이 비어 있습니다.");
        }
        return switch (trimmed) {
            case "JE", "JAVA" -> MinecraftEdition.JAVA.name();
            case "BE", "BEDROCK" -> MinecraftEdition.BEDROCK.name();
            default -> throw new IllegalArgumentException("지원하지 않는 minecraft edition입니다: " + trimmed);
        };
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }
        return trimmed;
    }

    private String normalizeReference(String value) {
        String trimmed = trimToNull(value);
        if (!StringUtils.hasText(trimmed)) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String stripBedrockPrefix(String uuid) {
        String trimmed = trimToNull(uuid);
        if (trimmed == null) {
            return null;
        }
        return trimmed.startsWith("be:") ? trimmed.substring(3) : trimmed;
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

    private LocalDateTime coalesce(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String deterministicId(String ownerMemberId, String normalizedKey) {
        return UUID.nameUUIDFromBytes((ownerMemberId + ":" + normalizedKey).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private final class MinecraftAccountInsertSetter implements BatchPreparedStatementSetter {
        private final List<MinecraftAccountRow> rows;

        private MinecraftAccountInsertSetter(List<MinecraftAccountRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            MinecraftAccountRow row = rows.get(i);
            ps.setString(1, row.id());
            ps.setString(2, row.ownerMemberId());
            ps.setString(3, row.parentAccountId());
            ps.setString(4, row.accountRole());
            ps.setString(5, row.edition());
            ps.setString(6, row.gameName());
            ps.setString(7, row.storedName());
            ps.setString(8, row.normalizedKey());
            ps.setString(9, row.avatarUuid());
            ps.setObject(10, row.lastSeenAt());
            ps.setTimestamp(11, timestamp(row.createdAt()));
            ps.setTimestamp(12, timestamp(row.updatedAt()));
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private record MinecraftAccountRow(
            String id,
            String ownerMemberId,
            String parentAccountId,
            String accountRole,
            String edition,
            String gameName,
            String storedName,
            String normalizedKey,
            String avatarUuid,
            Instant lastSeenAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    private record CanonicalMinecraftSource(
            String ownerMemberId,
            String referenceNickname,
            String edition,
            String nickname,
            String storedName,
            String uuid,
            String normalizedKey,
            String avatarUuid,
            String accountRole,
            String whoseFriend,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Instant lastSeenAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserMinecraftExportItem(
            String id,
            UserMinecraftExportData data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserMinecraftExportData(
            MinecraftAccountContainer minecraftAccount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MinecraftAccountContainer(
            List<EmbeddedMinecraftAccountExport> accounts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddedMinecraftAccountExport(
            String edition,
            String nickname,
            String storedName,
            String uuid,
            String whoseFriend,
            JsonNode linkedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MinecraftWhitelistExport(
            Map<String, WhitelistValue> players,
            Map<String, WhitelistValue> BEPlayers
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WhitelistValue(
            JsonNode addedAt,
            JsonNode lastSeenAt,
            String storedName
    ) {
    }

    private record WhitelistEntry(
            String normalizedKey,
            LocalDateTime addedAt,
            Instant lastSeenAt
    ) {
    }
}
