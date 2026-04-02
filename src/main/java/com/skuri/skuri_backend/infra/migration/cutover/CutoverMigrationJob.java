package com.skuri.skuri_backend.infra.migration.cutover;

import com.skuri.skuri_backend.infra.migration.FileHashUtils;
import com.skuri.skuri_backend.infra.migration.MigrationExecutionResult;
import com.skuri.skuri_backend.infra.migration.MigrationReject;
import com.skuri.skuri_backend.infra.migration.MigrationReportWriter;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import com.skuri.skuri_backend.infra.migration.MigrationSummary;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CutoverMigrationJob {

    private final MembersMigrationJob membersMigrationJob;
    private final TimetableMigrationJob timetableMigrationJob;
    private final MinecraftAccountMigrationJob minecraftAccountMigrationJob;
    private final MigrationReportWriter reportWriter;

    public CutoverMigrationJob(
            MembersMigrationJob membersMigrationJob,
            TimetableMigrationJob timetableMigrationJob,
            MinecraftAccountMigrationJob minecraftAccountMigrationJob,
            MigrationReportWriter reportWriter
    ) {
        this.membersMigrationJob = membersMigrationJob;
        this.timetableMigrationJob = timetableMigrationJob;
        this.minecraftAccountMigrationJob = minecraftAccountMigrationJob;
        this.reportWriter = reportWriter;
    }

    public MigrationExecutionResult execute(
            Path usersFile,
            Path coursesFile,
            Path timetablesFile,
            Path minecraftFile,
            MigrationRunOptions options
    ) {
        CutoverDomainResult membersResult = membersMigrationJob.execute(usersFile, options);
        CutoverDomainResult timetableResult = timetableMigrationJob.execute(usersFile, coursesFile, timetablesFile, options);
        CutoverDomainResult minecraftResult = minecraftAccountMigrationJob.execute(usersFile, minecraftFile, options);

        List<CutoverDomainResult> domainResults = List.of(membersResult, timetableResult, minecraftResult);
        List<MigrationReject> combinedRejects = new ArrayList<>();
        Map<String, Long> counters = new LinkedHashMap<>();
        Map<String, Object> artifacts = new LinkedHashMap<>();

        counters.put("users_file_count", 1L);
        counters.put("courses_file_count", 1L);
        counters.put("timetables_file_count", 1L);
        counters.put("minecraft_file_count", 1L);

        for (CutoverDomainResult domainResult : domainResults) {
            counters.putAll(domainResult.counters());
            combinedRejects.addAll(domainResult.rejects());
            artifacts.put(rejectArtifactName(domainResult.domain()), domainResult.rejects());
            artifacts.putAll(domainResult.artifacts());
        }

        counters.put("rejected", (long) combinedRejects.size());

        MigrationSummary summary = new MigrationSummary(
                "CUTOVER",
                options.mode().name(),
                usersFile + ", " + coursesFile + ", " + timetablesFile + ", " + minecraftFile,
                combinedFileHash(usersFile, coursesFile, timetablesFile, minecraftFile),
                options.startedAt(),
                LocalDateTime.now(),
                counters
        );

        Path reportDirectory = reportWriter.write("cutover", summary, combinedRejects, options.reportRootDirectory(), artifacts);
        if (!combinedRejects.isEmpty() && options.failOnReject()) {
            throw new IllegalStateException("컷오버 마이그레이션 reject가 발생했습니다. reportDirectory=" + reportDirectory);
        }

        return new MigrationExecutionResult(summary, reportDirectory, List.copyOf(combinedRejects));
    }

    private String combinedFileHash(Path usersFile, Path coursesFile, Path timetablesFile, Path minecraftFile) {
        return FileHashUtils.sha256(usersFile)
                + ":" + FileHashUtils.sha256(coursesFile)
                + ":" + FileHashUtils.sha256(timetablesFile)
                + ":" + FileHashUtils.sha256(minecraftFile);
    }

    private String rejectArtifactName(String domain) {
        return switch (domain) {
            case "members" -> "member-rejects.json";
            case "timetable" -> "timetable-rejects.json";
            case "minecraft" -> "minecraft-rejects.json";
            default -> domain + "-rejects.json";
        };
    }
}
