package com.skuri.skuri_backend.infra.admin.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.academic.dto.response.AcademicScheduleResponse;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerAdminResponse;
import com.skuri.skuri_backend.domain.campus.entity.CampusBanner;
import com.skuri.skuri_backend.domain.campus.repository.CampusBannerRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.entity.AppVersion;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.Report;
import com.skuri.skuri_backend.domain.support.repository.AppVersionRepository;
import com.skuri.skuri_backend.domain.support.repository.CafeteriaMenuRepository;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.domain.support.repository.LegalDocumentRepository;
import com.skuri.skuri_backend.domain.support.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component("adminAuditSnapshots")
@RequiredArgsConstructor
public class AdminAuditSnapshotFactory {

    private final AcademicScheduleRepository academicScheduleRepository;
    private final CourseRepository courseRepository;
    private final AppNoticeRepository appNoticeRepository;
    private final CampusBannerRepository campusBannerRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final InquiryRepository inquiryRepository;
    private final ReportRepository reportRepository;
    private final AppVersionRepository appVersionRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final CafeteriaMenuRepository cafeteriaMenuRepository;

    public AcademicScheduleResponse academicSchedule(String scheduleId) {
        return academicScheduleRepository.findById(scheduleId)
                .map(schedule -> new AcademicScheduleResponse(
                        schedule.getId(),
                        schedule.getTitle(),
                        schedule.getStartDate(),
                        schedule.getEndDate(),
                        schedule.getType(),
                        schedule.isPrimary(),
                        schedule.getDescription()
                ))
                .orElse(null);
    }

    public CourseSemesterSnapshot courseSemester(String semester) {
        if (semester == null || semester.isBlank()) {
            return null;
        }
        List<Course> courses = courseRepository.findAllBySemesterWithSchedules(semester);
        return new CourseSemesterSnapshot(
                semester,
                courses.size(),
                courses.stream()
                        .map(course -> new CourseSummarySnapshot(
                                course.getId(),
                                course.semesterCourseKey(),
                                course.getName(),
                                course.getDepartment(),
                                course.getProfessor(),
                                course.getGrade(),
                                course.getCredits(),
                                course.getSchedules().stream().map(this::toCourseScheduleSnapshot).toList()
                        ))
                        .toList()
        );
    }

    public ChatRoomSnapshot chatRoom(String chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .map(room -> new ChatRoomSnapshot(
                        room.getId(),
                        room.getName(),
                        room.getType(),
                        room.getDescription(),
                        room.isPublic(),
                        room.getCreatedBy(),
                        room.getMemberCount()
                ))
                .orElse(null);
    }

    public AppNoticeResponse appNotice(String appNoticeId) {
        return appNoticeRepository.findById(appNoticeId)
                .map(this::toAppNoticeResponse)
                .orElse(null);
    }

    public CampusBannerAdminResponse campusBanner(String bannerId) {
        return campusBannerRepository.findById(bannerId)
                .map(this::toCampusBannerResponse)
                .orElse(null);
    }

    public List<CampusBannerOrderSnapshot> campusBannerOrder() {
        return campusBannerRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc().stream()
                .map(banner -> new CampusBannerOrderSnapshot(
                        banner.getId(),
                        banner.getTitleLabel(),
                        banner.getDisplayOrder()
                ))
                .toList();
    }

    public InquirySnapshot inquiry(String inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .map(inquiry -> new InquirySnapshot(
                        inquiry.getId(),
                        inquiry.getUserId(),
                        inquiry.getType(),
                        inquiry.getSubject(),
                        inquiry.getContent(),
                        inquiry.getStatus(),
                        inquiry.getAdminMemo(),
                        inquiry.getCreatedAt(),
                        inquiry.getUpdatedAt()
                ))
                .orElse(null);
    }

    public ReportSnapshot report(String reportId) {
        return reportRepository.findById(reportId)
                .map(report -> new ReportSnapshot(
                        report.getId(),
                        report.getReporterId(),
                        report.getTargetType(),
                        report.getTargetId(),
                        report.getTargetAuthorId(),
                        report.getCategory(),
                        report.getReason(),
                        report.getStatus(),
                        report.getAction(),
                        report.getAdminMemo(),
                        report.getCreatedAt(),
                        report.getUpdatedAt()
                ))
                .orElse(null);
    }

    public AppVersionSnapshot appVersion(String platform) {
        return appVersionRepository.findById(platform)
                .map(appVersion -> new AppVersionSnapshot(
                        appVersion.getPlatform(),
                        appVersion.getMinimumVersion(),
                        appVersion.isForceUpdate(),
                        appVersion.getMessage(),
                        appVersion.getTitle(),
                        appVersion.isShowButton(),
                        appVersion.getButtonText(),
                        appVersion.getButtonUrl(),
                        appVersion.getUpdatedAt()
                ))
                .orElse(null);
    }

    public LegalDocumentAdminResponse legalDocument(String documentKey) {
        return legalDocumentRepository.findById(documentKey)
                .map(legalDocument -> new LegalDocumentAdminResponse(
                        legalDocument.getDocumentKey(),
                        legalDocument.getTitle(),
                        new com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner(
                                legalDocument.getBannerIconKey(),
                                List.copyOf(legalDocument.getBannerLines()),
                                legalDocument.getBannerTitle(),
                                legalDocument.getBannerTone()
                        ),
                        List.copyOf(legalDocument.getSections()),
                        List.copyOf(legalDocument.getFooterLines()),
                        legalDocument.isActive(),
                        legalDocument.getCreatedAt(),
                        legalDocument.getUpdatedAt()
                ))
                .orElse(null);
    }

    public CafeteriaMenuResponse cafeteriaMenu(String weekId) {
        return cafeteriaMenuRepository.findById(weekId)
                .map(menu -> new CafeteriaMenuResponse(
                        menu.getWeekId(),
                        menu.getWeekStart(),
                        menu.getWeekEnd(),
                        menu.getMenus()
                ))
                .orElse(null);
    }

    private CourseScheduleSnapshot toCourseScheduleSnapshot(CourseSchedule schedule) {
        return new CourseScheduleSnapshot(schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod());
    }

    private AppNoticeResponse toAppNoticeResponse(AppNotice appNotice) {
        return new AppNoticeResponse(
                appNotice.getId(),
                appNotice.getTitle(),
                appNotice.getContent(),
                appNotice.getCategory(),
                appNotice.getPriority(),
                List.copyOf(appNotice.getImageUrls()),
                appNotice.getActionUrl(),
                appNotice.getPublishedAt(),
                appNotice.getCreatedAt(),
                appNotice.getUpdatedAt()
        );
    }

    private CampusBannerAdminResponse toCampusBannerResponse(CampusBanner campusBanner) {
        return new CampusBannerAdminResponse(
                campusBanner.getId(),
                campusBanner.getBadgeLabel(),
                campusBanner.getTitleLabel(),
                campusBanner.getDescriptionLabel(),
                campusBanner.getButtonLabel(),
                campusBanner.getPaletteKey(),
                campusBanner.getImageUrl(),
                campusBanner.getActionType(),
                campusBanner.getActionTarget(),
                campusBanner.getActionParams() == null
                        ? null
                        : ObjectMapperConfig.SHARED_OBJECT_MAPPER.convertValue(
                                campusBanner.getActionParams().deepCopy(),
                                new TypeReference<Map<String, Object>>() {
                                }
                        ),
                campusBanner.getActionUrl(),
                campusBanner.isActive(),
                campusBanner.getDisplayStartAt(),
                campusBanner.getDisplayEndAt(),
                campusBanner.getDisplayOrder(),
                campusBanner.getCreatedAt(),
                campusBanner.getUpdatedAt()
        );
    }

    public record CourseSemesterSnapshot(
            String semester,
            int totalCourses,
            List<CourseSummarySnapshot> courses
    ) {
    }

    public record CourseSummarySnapshot(
            String id,
            String courseKey,
            String name,
            String department,
            String professor,
            Integer grade,
            Integer credits,
            List<CourseScheduleSnapshot> schedules
    ) {
    }

    public record CourseScheduleSnapshot(
            int dayOfWeek,
            int startPeriod,
            int endPeriod
    ) {
    }

    public record ChatRoomSnapshot(
            String id,
            String name,
            Object type,
            String description,
            boolean isPublic,
            String createdBy,
            int memberCount
    ) {
    }

    public record InquirySnapshot(
            String id,
            String memberId,
            Object type,
            String subject,
            String content,
            Object status,
            String memo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record ReportSnapshot(
            String id,
            String reporterId,
            Object targetType,
            String targetId,
            String targetAuthorId,
            String category,
            String reason,
            Object status,
            String action,
            String memo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AppVersionSnapshot(
            String platform,
            String minimumVersion,
            boolean forceUpdate,
            String message,
            String title,
            boolean showButton,
            String buttonText,
            String buttonUrl,
            LocalDateTime updatedAt
    ) {
    }

    public record CampusBannerOrderSnapshot(
            String id,
            String titleLabel,
            int displayOrder
    ) {
    }
}
