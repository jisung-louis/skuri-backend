package com.skuri.skuri_backend.domain.support.repository;

import com.skuri.skuri_backend.domain.support.entity.CafeteriaMenu;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CafeteriaMenuRepository extends JpaRepository<CafeteriaMenu, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CafeteriaMenu c where c.weekId = :weekId")
    Optional<CafeteriaMenu> findByWeekIdForUpdate(@Param("weekId") String weekId);
}
