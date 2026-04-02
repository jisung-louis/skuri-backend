package com.skuri.skuri_backend.infra.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MigrationReportWriter {

    private static final DateTimeFormatter DIRECTORY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ObjectMapper objectMapper;

    public MigrationReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path write(
            String planName,
            MigrationSummary summary,
            List<MigrationReject> rejects,
            Path reportRootDirectory
    ) {
        try {
            Files.createDirectories(reportRootDirectory);
            Path reportDirectory = reportRootDirectory.resolve(directoryName(planName, summary.startedAt()));
            Files.createDirectories(reportDirectory);

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(reportDirectory.resolve("summary.json").toFile(), summary);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(reportDirectory.resolve("rejects.json").toFile(), rejects);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(
                            reportDirectory.resolve("metadata.json").toFile(),
                            Map.of(
                                    "plan", summary.plan(),
                                    "mode", summary.mode(),
                                    "generatedAt", LocalDateTime.now()
                            )
                    );
            return reportDirectory;
        } catch (IOException e) {
            log.error("마이그레이션 리포트 기록에 실패했습니다. plan={}", planName, e);
            throw new IllegalStateException("마이그레이션 리포트 기록에 실패했습니다.", e);
        }
    }

    private String directoryName(String planName, LocalDateTime startedAt) {
        return planName.toLowerCase() + "-" + DIRECTORY_TIMESTAMP_FORMATTER.format(startedAt);
    }
}
