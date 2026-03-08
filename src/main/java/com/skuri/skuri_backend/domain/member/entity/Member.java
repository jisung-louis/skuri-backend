package com.skuri.skuri_backend.domain.member.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {
    private static final String DEFAULT_NICKNAME = "스쿠리 유저";

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(length = 50)
    private String department;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(length = 50)
    private String realname;

    @Column(name = "is_admin")
    private boolean isAdmin;

    @Embedded
    private BankAccount bankAccount;

    @Embedded
    private NotificationSetting notificationSetting;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    private Member(String id, String email, String realname, LocalDateTime joinedAt) {
        this.id = id;
        this.email = email;
        this.nickname = DEFAULT_NICKNAME;
        this.realname = realname;
        this.joinedAt = joinedAt;
        this.lastLogin = joinedAt;
        this.notificationSetting = NotificationSetting.defaultSetting();
    }

    public static Member create(String id, String email, String realname, LocalDateTime joinedAt) {
        return new Member(id, email, realname, joinedAt);
    }

    @PrePersist
    public void prePersist() {
        if (notificationSetting == null) {
            notificationSetting = NotificationSetting.defaultSetting();
        } else {
            notificationSetting.backfillAcademicScheduleDefaults();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        if (lastLogin == null) {
            lastLogin = joinedAt;
        }
    }

    public void updateLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void updateProfile(String nickname, String studentId, String department, String photoUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (studentId != null) {
            this.studentId = studentId;
        }
        if (department != null) {
            this.department = department;
        }
        if (photoUrl != null) {
            this.photoUrl = photoUrl;
        }
    }

    public void updateBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public void updateNotificationSetting(
            Boolean allNotifications,
            Boolean partyNotifications,
            Boolean noticeNotifications,
            Boolean boardLikeNotifications,
            Boolean commentNotifications,
            Boolean bookmarkedPostCommentNotifications,
            Boolean systemNotifications,
            Boolean academicScheduleNotifications,
            Boolean academicScheduleDayBeforeEnabled,
            Boolean academicScheduleAllEventsEnabled,
            java.util.Map<String, Boolean> noticeNotificationsDetail
    ) {
        if (this.notificationSetting == null) {
            this.notificationSetting = NotificationSetting.defaultSetting();
        } else {
            this.notificationSetting.backfillAcademicScheduleDefaults();
        }
        this.notificationSetting.apply(
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
                noticeNotificationsDetail
        );
    }

    public boolean hasUnsetNotificationSettingDefaults() {
        return notificationSetting == null || notificationSetting.hasUnsetAcademicScheduleDefaults();
    }

    public void backfillNotificationSettingDefaults() {
        if (notificationSetting == null) {
            notificationSetting = NotificationSetting.defaultSetting();
            return;
        }

        notificationSetting.backfillAcademicScheduleDefaults();
    }
}
