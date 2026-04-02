package com.skuri.skuri_backend.infra.migration.cutover;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.academic.entity.Course;
import com.skuri.skuri_backend.domain.academic.entity.CourseSchedule;
import com.skuri.skuri_backend.domain.academic.repository.CourseRepository;
import com.skuri.skuri_backend.infra.migration.FirestoreTimestampParser;
import com.skuri.skuri_backend.infra.migration.MigrationMode;
import com.skuri.skuri_backend.infra.migration.MigrationReject;
import com.skuri.skuri_backend.infra.migration.MigrationRunOptions;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TimetableMigrationJob {

    private static final String INSERT_TIMETABLE_SQL = """
            insert into user_timetables (id, user_id, semester, created_at, updated_at)
            values (?, ?, ?, ?, ?)
            """;

    private static final String INSERT_TIMETABLE_COURSE_SQL = """
            insert into user_timetable_courses (timetable_id, course_id)
            values (?, ?)
            """;

    private final ObjectMapper objectMapper;
    private final FirestoreTimestampParser timestampParser;
    private final CourseRepository courseRepository;
    private final JdbcTemplate jdbcTemplate;

    public TimetableMigrationJob(
            ObjectMapper objectMapper,
            FirestoreTimestampParser timestampParser,
            CourseRepository courseRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.objectMapper = objectMapper;
        this.timestampParser = timestampParser;
        this.courseRepository = courseRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public CutoverDomainResult execute(
            Path usersFile,
            Path coursesFile,
            Path timetablesFile,
            MigrationRunOptions options
    ) {
        Set<String> knownUserIds = readUsers(usersFile).stream()
                .map(UserIdOnlyExportItem::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<CourseExportItem> courseExports = readCourses(coursesFile);
        Map<String, CourseExportData> sourceCoursesById = courseExports.stream()
                .collect(Collectors.toMap(CourseExportItem::id, CourseExportItem::data, (left, right) -> left, LinkedHashMap::new));
        List<TimetableExportItem> timetableExports = readTimetables(timetablesFile);

        Set<String> referencedCourseIds = timetableExports.stream()
                .flatMap(item -> {
                    if (item.data() == null || item.data().courses() == null) {
                        return List.<String>of().stream();
                    }
                    return item.data().courses().stream();
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> referencedSemesters = timetableExports.stream()
                .map(item -> item.data().semester())
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<LiveCourseCandidate> liveCourses = referencedSemesters.stream()
                .flatMap(semester -> courseRepository.findAllBySemesterWithSchedules(semester).stream())
                .map(LiveCourseCandidate::from)
                .toList();

        Map<String, CourseMatchResult> courseMatchResults = resolveCourses(referencedCourseIds, sourceCoursesById, liveCourses);
        List<CourseMatchReportRow> courseMatchReportRows = courseMatchResults.values().stream()
                .map(CourseMatchResult::reportRow)
                .sorted((left, right) -> left.firestoreCourseId().compareTo(right.firestoreCourseId()))
                .toList();

        List<MigrationReject> rejects = new ArrayList<>();
        List<TimetableRow> timetableRows = new ArrayList<>();
        List<TimetableCourseRow> courseRows = new ArrayList<>();

        for (TimetableExportItem item : timetableExports) {
            TimetableExportData data = item.data();
            if (data == null) {
                rejects.add(new MigrationReject(item.id(), "data가 비어 있습니다.", Map.of("domain", "timetable")));
                continue;
            }

            String timetableId = trimToNull(item.id());
            String userId = trimToNull(data.userId());
            String semester = trimToNull(data.semester());
            if (!StringUtils.hasText(timetableId) || !StringUtils.hasText(userId) || !StringUtils.hasText(semester)) {
                rejects.add(new MigrationReject(item.id(), "timetable 필수 값(id/userId/semester)이 비어 있습니다.", Map.of("domain", "timetable")));
                continue;
            }
            if (!knownUserIds.contains(userId)) {
                rejects.add(new MigrationReject(
                        timetableId,
                        "users export에 없는 userId를 참조합니다. 컷오프 정책에 따라 버립니다.",
                        Map.of("domain", "timetable", "userId", userId, "semester", semester)
                ));
                continue;
            }

            List<String> sourceCourseIds = data.courses() == null ? List.of() : data.courses().stream()
                    .map(this::trimToNull)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();

            List<String> unresolvedCourseIds = sourceCourseIds.stream()
                    .filter(courseId -> !courseMatchResults.containsKey(courseId) || !courseMatchResults.get(courseId).resolved())
                    .toList();
            if (!unresolvedCourseIds.isEmpty()) {
                rejects.add(new MigrationReject(
                        timetableId,
                        "매칭할 수 없는 과목이 포함되어 있습니다.",
                        Map.of("domain", "timetable", "userId", userId, "semester", semester, "unresolvedCourseIds", unresolvedCourseIds)
                ));
                continue;
            }

            LocalDateTime createdAt = coalesce(
                    timestampParser.toLocalDateTime(data.createdAt()),
                    timestampParser.toLocalDateTime(data.updatedAt()),
                    options.startedAt()
            );
            LocalDateTime updatedAt = coalesce(
                    timestampParser.toLocalDateTime(data.updatedAt()),
                    createdAt
            );
            timetableRows.add(new TimetableRow(timetableId, userId, semester, createdAt, updatedAt));
            for (String sourceCourseId : sourceCourseIds) {
                courseRows.add(new TimetableCourseRow(timetableId, courseMatchResults.get(sourceCourseId).mysqlCourseId()));
            }
        }

        long existingCount = timetableRows.stream()
                .filter(row -> timetableExists(row.userId(), row.semester()))
                .count();
        long insertedCount = timetableRows.size() - existingCount;

        if (options.mode() == MigrationMode.APPLY && !timetableRows.isEmpty()) {
            for (TimetableRow row : timetableRows) {
                deleteTimetableByUserAndSemester(row.userId(), row.semester());
                jdbcTemplate.update(
                        INSERT_TIMETABLE_SQL,
                        row.id(),
                        row.userId(),
                        row.semester(),
                        timestamp(row.createdAt()),
                        timestamp(row.updatedAt())
                );
            }
            if (!courseRows.isEmpty()) {
                jdbcTemplate.batchUpdate(INSERT_TIMETABLE_COURSE_SQL, new TimetableCourseInsertSetter(courseRows));
            }
        }

        return new CutoverDomainResult(
                "timetable",
                Map.of(
                        "timetables_scanned", (long) timetableExports.size(),
                        "timetables_inserted", insertedCount,
                        "timetables_updated", existingCount,
                        "timetable_courses_inserted", (long) courseRows.size()
                ),
                List.copyOf(rejects),
                Map.of("course-matches.json", courseMatchReportRows)
        );
    }

    private List<UserIdOnlyExportItem> readUsers(Path usersFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(usersFile), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("users export를 읽는 중 실패했습니다: " + usersFile, e);
        }
    }

    private List<CourseExportItem> readCourses(Path coursesFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(coursesFile), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("courses export를 읽는 중 실패했습니다: " + coursesFile, e);
        }
    }

    private List<TimetableExportItem> readTimetables(Path timetablesFile) {
        try {
            return objectMapper.readValue(Files.newInputStream(timetablesFile), new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("userTimetables export를 읽는 중 실패했습니다: " + timetablesFile, e);
        }
    }

    private Map<String, CourseMatchResult> resolveCourses(
            Collection<String> referencedCourseIds,
            Map<String, CourseExportData> sourceCoursesById,
            List<LiveCourseCandidate> liveCourses
    ) {
        Map<String, CourseMatchResult> results = new LinkedHashMap<>();
        for (String firestoreCourseId : referencedCourseIds) {
            CourseExportData source = sourceCoursesById.get(firestoreCourseId);
            if (source == null) {
                results.put(firestoreCourseId, CourseMatchResult.unresolved(
                        firestoreCourseId,
                        null,
                        "MISSING_SOURCE_COURSE",
                        List.of()
                ));
                continue;
            }

            List<LiveCourseCandidate> exactCandidates = liveCourses.stream()
                    .filter(candidate -> candidate.semester().equals(source.semester()))
                    .filter(candidate -> candidate.code().equals(source.code()))
                    .filter(candidate -> candidate.division().equals(source.division()))
                    .toList();
            if (exactCandidates.size() == 1) {
                results.put(firestoreCourseId, CourseMatchResult.resolved(firestoreCourseId, source, exactCandidates.getFirst(), "EXACT_SEMESTER_CODE_DIVISION"));
                continue;
            }

            List<LiveCourseCandidate> candidates = liveCourses.stream()
                    .filter(candidate -> candidate.semester().equals(source.semester()))
                    .filter(candidate -> candidate.code().equals(source.code()))
                    .filter(candidate -> candidate.name().equals(source.name()))
                    .toList();
            String strategy = "SEMESTER_CODE_NAME";

            candidates = narrowByDepartment(candidates, source.department());
            if (candidates.size() == 1) {
                results.put(firestoreCourseId, CourseMatchResult.resolved(firestoreCourseId, source, candidates.getFirst(), "SEMESTER_CODE_NAME_DEPARTMENT"));
                continue;
            }

            String sourceScheduleSignature = scheduleSignature(source.schedule());
            if (StringUtils.hasText(sourceScheduleSignature)) {
                List<LiveCourseCandidate> narrowedBySchedule = candidates.stream()
                        .filter(candidate -> sourceScheduleSignature.equals(candidate.scheduleSignature()))
                        .toList();
                if (!narrowedBySchedule.isEmpty()) {
                    candidates = narrowedBySchedule;
                    strategy = "SEMESTER_CODE_NAME_DEPARTMENT_SCHEDULE";
                }
            }
            if (candidates.size() == 1) {
                results.put(firestoreCourseId, CourseMatchResult.resolved(firestoreCourseId, source, candidates.getFirst(), strategy));
                continue;
            }

            String sourceLocation = normalizeText(source.location());
            if (StringUtils.hasText(sourceLocation)) {
                List<LiveCourseCandidate> narrowedByLocation = candidates.stream()
                        .filter(candidate -> sourceLocation.equals(candidate.location()))
                        .toList();
                if (!narrowedByLocation.isEmpty()) {
                    candidates = narrowedByLocation;
                    strategy = strategy + "_LOCATION";
                }
            }
            if (candidates.size() == 1) {
                results.put(firestoreCourseId, CourseMatchResult.resolved(firestoreCourseId, source, candidates.getFirst(), strategy));
                continue;
            }

            results.put(firestoreCourseId, CourseMatchResult.unresolved(
                    firestoreCourseId,
                    source,
                    candidates.isEmpty() ? "NO_MATCH" : "AMBIGUOUS_MATCH",
                    candidates
            ));
        }
        return results;
    }

    private List<LiveCourseCandidate> narrowByDepartment(List<LiveCourseCandidate> candidates, String department) {
        String normalizedDepartment = normalizeText(department);
        if (!StringUtils.hasText(normalizedDepartment)) {
            return candidates;
        }
        List<LiveCourseCandidate> narrowed = candidates.stream()
                .filter(candidate -> normalizedDepartment.equals(candidate.department()))
                .toList();
        return narrowed.isEmpty() ? candidates : narrowed;
    }

    private boolean timetableExists(String userId, String semester) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_timetables where user_id = ? and semester = ?",
                Integer.class,
                userId,
                semester
        );
        return count != null && count > 0;
    }

    private void deleteTimetableByUserAndSemester(String userId, String semester) {
        jdbcTemplate.update(
                """
                        delete from user_timetable_manual_courses
                        where timetable_id in (
                            select id
                            from user_timetables
                            where user_id = ? and semester = ?
                        )
                        """,
                userId,
                semester
        );
        jdbcTemplate.update(
                """
                        delete from user_timetable_courses
                        where timetable_id in (
                            select id
                            from user_timetables
                            where user_id = ? and semester = ?
                        )
                        """,
                userId,
                semester
        );
        jdbcTemplate.update(
                "delete from user_timetables where user_id = ? and semester = ?",
                userId,
                semester
        );
    }

    private static String scheduleSignature(List<CourseScheduleExportData> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return null;
        }
        return schedules.stream()
                .map(schedule -> schedule.dayOfWeek() + ":" + schedule.startPeriod() + "-" + schedule.endPeriod())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private LocalDateTime coalesce(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private final class TimetableCourseInsertSetter implements BatchPreparedStatementSetter {
        private final List<TimetableCourseRow> rows;

        private TimetableCourseInsertSetter(List<TimetableCourseRow> rows) {
            this.rows = rows;
        }

        @Override
        public void setValues(PreparedStatement ps, int i) throws SQLException {
            TimetableCourseRow row = rows.get(i);
            ps.setString(1, row.timetableId());
            ps.setString(2, row.courseId());
        }

        @Override
        public int getBatchSize() {
            return rows.size();
        }
    }

    private record TimetableRow(
            String id,
            String userId,
            String semester,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    private record TimetableCourseRow(
            String timetableId,
            String courseId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserIdOnlyExportItem(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TimetableExportItem(
            String id,
            TimetableExportData data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TimetableExportData(
            String userId,
            String semester,
            JsonNode createdAt,
            JsonNode updatedAt,
            List<String> courses
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CourseExportItem(
            String id,
            CourseExportData data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CourseExportData(
            String semester,
            String code,
            String division,
            String name,
            String department,
            String location,
            List<CourseScheduleExportData> schedule
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CourseScheduleExportData(
            Integer dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    ) {
    }

    private record LiveCourseCandidate(
            String id,
            String semester,
            String code,
            String division,
            String name,
            String department,
            String location,
            String scheduleSignature
    ) {
        private static LiveCourseCandidate from(Course course) {
            return new LiveCourseCandidate(
                    course.getId(),
                    course.getSemester(),
                    course.getCode(),
                    course.getDivision(),
                    course.getName(),
                    normalize(course.getDepartment()),
                    normalize(course.getLocation()),
                    scheduleSignature(course.getSchedules())
            );
        }

        private static String normalize(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
        }

        private static String scheduleSignature(List<CourseSchedule> schedules) {
            if (schedules == null || schedules.isEmpty()) {
                return null;
            }
            return schedules.stream()
                    .map(schedule -> schedule.getDayOfWeek() + ":" + schedule.getStartPeriod() + "-" + schedule.getEndPeriod())
                    .sorted()
                    .collect(Collectors.joining("|"));
        }
    }

    private record CourseMatchResult(
            String firestoreCourseId,
            String mysqlCourseId,
            boolean resolved,
            String strategy,
            CourseMatchReportRow reportRow
    ) {
        private static CourseMatchResult resolved(
                String firestoreCourseId,
                CourseExportData source,
                LiveCourseCandidate liveCourse,
                String strategy
        ) {
            return new CourseMatchResult(
                    firestoreCourseId,
                    liveCourse.id(),
                    true,
                    strategy,
                    new CourseMatchReportRow(
                            firestoreCourseId,
                            liveCourse.id(),
                            true,
                            strategy,
                            source == null ? null : source.semester(),
                            source == null ? null : source.code(),
                            source == null ? null : source.division(),
                            source == null ? null : source.name(),
                            source == null ? null : source.department(),
                            source == null ? null : source.location(),
                            source == null ? null : scheduleSignature(source.schedule()),
                            List.of()
                    )
            );
        }

        private static CourseMatchResult unresolved(
                String firestoreCourseId,
                CourseExportData source,
                String strategy,
                List<LiveCourseCandidate> candidates
        ) {
            return new CourseMatchResult(
                    firestoreCourseId,
                    null,
                    false,
                    strategy,
                    new CourseMatchReportRow(
                            firestoreCourseId,
                            null,
                            false,
                            strategy,
                            source == null ? null : source.semester(),
                            source == null ? null : source.code(),
                            source == null ? null : source.division(),
                            source == null ? null : source.name(),
                            source == null ? null : source.department(),
                            source == null ? null : source.location(),
                            source == null ? null : scheduleSignature(source.schedule()),
                            candidates.stream()
                                    .map(candidate -> {
                                        Map<String, Object> values = new LinkedHashMap<>();
                                        values.put("mysqlCourseId", candidate.id());
                                        values.put("semester", candidate.semester());
                                        values.put("code", candidate.code());
                                        values.put("division", candidate.division());
                                        values.put("name", candidate.name());
                                        values.put("department", candidate.department());
                                        values.put("location", candidate.location());
                                        values.put("scheduleSignature", candidate.scheduleSignature());
                                        return values;
                                    })
                                    .toList()
                    )
            );
        }
    }

    private record CourseMatchReportRow(
            String firestoreCourseId,
            String mysqlCourseId,
            boolean resolved,
            String strategy,
            String sourceSemester,
            String sourceCode,
            String sourceDivision,
            String sourceName,
            String sourceDepartment,
            String sourceLocation,
            String sourceScheduleSignature,
            List<Map<String, Object>> candidates
    ) {
    }
}
