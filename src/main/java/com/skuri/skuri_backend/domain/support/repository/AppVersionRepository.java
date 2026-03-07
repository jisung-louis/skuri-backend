package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppVersionRepository extends JpaRepository<AppVersion, String> {
}
