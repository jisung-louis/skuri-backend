package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, String>, MemberRepositoryCustom {

    @Query("""
            select m
            from Member m
            where m.id = :memberId
              and (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
            """)
    Optional<Member> findActiveById(@Param("memberId") String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    Optional<Member> findByIdForUpdate(@Param("memberId") String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from Member m
            where m.id = :memberId
              and (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
            """)
    Optional<Member> findActiveByIdForUpdate(@Param("memberId") String memberId);

    @Query("""
            select m.id
            from Member m
            where m.id <> :excludedId
              and (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.partyNotifications = true
            """)
    List<String> findPartyNotificationRecipientIdsExcluding(@Param("excludedId") String excludedId);

    @Query("""
            select m.id
            from Member m
            where m.id in :memberIds
              and (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.partyNotifications = true
            """)
    List<String> findPartyNotificationRecipientIds(@Param("memberIds") Collection<String> memberIds);

    @Query("""
            select m
            from Member m
            where (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.noticeNotifications = true
            """)
    List<Member> findMembersWithNoticeNotificationsEnabled();

    @Query("""
            select m.id
            from Member m
            where (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
            """)
    List<String> findAllMemberIds();

    @Query("""
            select m.id
            from Member m
            where (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
              and m.notificationSetting.allNotifications = true
              and m.notificationSetting.systemNotifications = true
            """)
    List<String> findSystemNotificationRecipientIds();

    @Query("""
            select m.id
            from Member m
            where (m.status = com.skuri.skuri_backend.domain.member.entity.MemberStatus.ACTIVE
                   or m.status is null)
              and m.notificationSetting.allNotifications = true
              and coalesce(m.notificationSetting.academicScheduleNotifications, true) = true
              and (:requireDayBefore = false
                   or coalesce(m.notificationSetting.academicScheduleDayBeforeEnabled, true) = true)
              and (:requireAllEvents = false
                   or coalesce(m.notificationSetting.academicScheduleAllEventsEnabled, false) = true)
            """)
    List<String> findAcademicScheduleReminderRecipientIds(
            @Param("requireDayBefore") boolean requireDayBefore,
            @Param("requireAllEvents") boolean requireAllEvents
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Member m
            set m.status = :activeStatus
            where m.status is null
            """)
    int backfillLegacyMembersAsActive(@Param("activeStatus") MemberStatus activeStatus);
}
