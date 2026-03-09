package com.skuri.skuri_backend.domain.support.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.MemberWithdrawalSanitizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_name", length = 50)
    private String userName;

    @Column(name = "user_realname", length = 50)
    private String userRealname;

    @Column(name = "user_student_id", length = 20)
    private String userStudentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status;

    @Column(name = "admin_memo", length = 500)
    private String adminMemo;

    private Inquiry(
            InquiryType type,
            String subject,
            String content,
            String userId,
            String userEmail,
            String userName,
            String userRealname,
            String userStudentId
    ) {
        this.type = type;
        this.subject = subject;
        this.content = content;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.userRealname = userRealname;
        this.userStudentId = userStudentId;
        this.status = InquiryStatus.PENDING;
    }

    public static Inquiry create(
            InquiryType type,
            String subject,
            String content,
            String userId,
            String userEmail,
            String userName,
            String userRealname,
            String userStudentId
    ) {
        return new Inquiry(type, subject, content, userId, userEmail, userName, userRealname, userStudentId);
    }

    public void updateStatus(InquiryStatus status, String adminMemo) {
        if (!this.status.canTransitionTo(status)) {
            throw new BusinessException(ErrorCode.INVALID_INQUIRY_STATUS_TRANSITION);
        }
        this.status = status;
        this.adminMemo = adminMemo;
    }

    public void anonymizeUserProfile() {
        this.userEmail = null;
        this.userName = MemberWithdrawalSanitizer.WITHDRAWN_DISPLAY_NAME;
        this.userRealname = null;
        this.userStudentId = null;
    }
}
