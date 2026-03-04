package com.skuri.skuri_backend.infra.auth.config;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("local-emulator")
@RequiredArgsConstructor
public class FirebaseAuthEmulatorShutdownHook {

    private final FirebaseAuthProperties properties;

    @PreDestroy
    void stopEmulatorIfManaged() {
        if (!properties.isUseEmulator() || !properties.isAutoStopOnShutdown()) {
            return;
        }

        String pidFile = properties.getEmulatorPidFile();
        if (!StringUtils.hasText(pidFile)) {
            return;
        }

        Path pidPath = Path.of(pidFile);
        if (!Files.exists(pidPath)) {
            return;
        }

        try {
            String pidValue = Files.readString(pidPath).trim();
            if (!StringUtils.hasText(pidValue)) {
                Files.deleteIfExists(pidPath);
                return;
            }

            long pid = Long.parseLong(pidValue);
            Optional<ProcessHandle> processHandle = ProcessHandle.of(pid);
            if (processHandle.isEmpty()) {
                Files.deleteIfExists(pidPath);
                return;
            }

            ProcessHandle process = processHandle.get();
            if (!process.isAlive()) {
                Files.deleteIfExists(pidPath);
                return;
            }

            process.descendants().forEach(ProcessHandle::destroy);
            process.destroy();

            try {
                process.onExit().get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }

            Files.deleteIfExists(pidPath);
            log.info("Stopped Firebase Auth emulator process on shutdown. pid={}", pid);
        } catch (NumberFormatException e) {
            log.warn("Invalid emulator PID value in {}. skip stop.", pidPath);
        } catch (IOException e) {
            log.warn("Failed to stop Firebase Auth emulator by pid file. file={}", pidPath, e);
        }
    }
}
