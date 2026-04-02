package com.skuri.skuri_backend.infra.migration;

import com.skuri.skuri_backend.infra.migration.notice.NoticeMigrationJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "migration", name = "enabled", havingValue = "true")
public class MigrationRunner implements ApplicationRunner {

    private final MigrationProperties migrationProperties;
    private final NoticeMigrationJob noticeMigrationJob;
    private final ConfigurableApplicationContext applicationContext;

    public MigrationRunner(
            MigrationProperties migrationProperties,
            NoticeMigrationJob noticeMigrationJob,
            ConfigurableApplicationContext applicationContext
    ) {
        this.migrationProperties = migrationProperties;
        this.noticeMigrationJob = noticeMigrationJob;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        MigrationRunOptions options = new MigrationRunOptions(
                migrationProperties.getMode(),
                Math.max(migrationProperties.getBatchSize(), 1),
                migrationProperties.isFailOnReject(),
                migrationProperties.getReportDir(),
                LocalDateTime.now()
        );

        MigrationExecutionResult result = switch (migrationProperties.getPlan()) {
            case NOTICES -> noticeMigrationJob.execute(requireReadableFile(migrationProperties.getNoticeFile(), "migration.notice-file"), options);
            case CUTOVER -> throw new IllegalStateException("migration.plan=CUTOVER 는 아직 구현되지 않았습니다.");
        };

        log.info(
                "마이그레이션 완료. plan={}, mode={}, reportDirectory={}, counters={}",
                result.summary().plan(),
                result.summary().mode(),
                result.reportDirectory(),
                result.summary().counters()
        );

        SpringApplication.exit(applicationContext, () -> 0);
    }

    private Path requireReadableFile(Path path, String propertyName) {
        if (path == null) {
            throw new IllegalArgumentException(propertyName + " 값이 필요합니다.");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException(propertyName + " 파일을 읽을 수 없습니다: " + path);
        }
        return path;
    }
}
