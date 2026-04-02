package com.skuri.skuri_backend.infra.migration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    private boolean enabled = false;

    private MigrationPlan plan = MigrationPlan.NOTICES;

    private MigrationMode mode = MigrationMode.DRY_RUN;

    private int batchSize = 200;

    private boolean failOnReject = true;

    private Path reportDir = Path.of("data-to-migration", "reports");

    private Path noticeFile;

    private Path usersFile;

    private Path coursesFile;

    private Path timetablesFile;

    private Path minecraftFile;
}
