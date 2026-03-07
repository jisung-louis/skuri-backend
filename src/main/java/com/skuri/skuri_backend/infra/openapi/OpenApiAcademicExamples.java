package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiAcademicExamples {

    private OpenApiAcademicExamples() {
    }

    public static final String SUCCESS_COURSE_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "course_uuid",
                    "semester": "2026-1",
                    "code": "01255",
                    "division": "001",
                    "name": "민법총칙",
                    "credits": 3,
                    "professor": "문상혁",
                    "department": "법학과",
                    "grade": 2,
                    "category": "전공선택",
                    "location": "영401",
                    "note": null,
                    "schedule": [
                      {
                        "dayOfWeek": 1,
                        "startPeriod": 3,
                        "endPeriod": 4
                      },
                      {
                        "dayOfWeek": 3,
                        "startPeriod": 3,
                        "endPeriod": 4
                      }
                    ]
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_TIMETABLE = """
            {
              "success": true,
              "data": {
                "id": "timetable_uuid",
                "semester": "2026-1",
                "courseCount": 1,
                "totalCredits": 3,
                "courses": [
                  {
                    "id": "course_uuid",
                    "code": "01255",
                    "division": "001",
                    "name": "민법총칙",
                    "professor": "문상혁",
                    "location": "영401",
                    "category": "전공선택",
                    "credits": 3,
                    "schedule": [
                      {
                        "dayOfWeek": 1,
                        "startPeriod": 3,
                        "endPeriod": 4
                      }
                    ]
                  }
                ],
                "slots": [
                  {
                    "courseId": "course_uuid",
                    "courseName": "민법총칙",
                    "code": "01255",
                    "dayOfWeek": 1,
                    "startPeriod": 3,
                    "endPeriod": 4,
                    "professor": "문상혁",
                    "location": "영401"
                  }
                ]
              }
            }
            """;

    public static final String SUCCESS_ACADEMIC_SCHEDULE_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "schedule_uuid",
                  "title": "1학기 개강",
                  "startDate": "2026-03-02",
                  "endDate": "2026-03-02",
                  "type": "SINGLE",
                  "isPrimary": true,
                  "description": "2026학년도 1학기 개강일"
                },
                {
                  "id": "schedule_uuid_2",
                  "title": "중간고사",
                  "startDate": "2026-04-15",
                  "endDate": "2026-04-21",
                  "type": "MULTI",
                  "isPrimary": true,
                  "description": "2026학년도 1학기 중간고사"
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_ACADEMIC_SCHEDULE = """
            {
              "success": true,
              "data": {
                "id": "schedule_uuid",
                "title": "중간고사",
                "startDate": "2026-04-15",
                "endDate": "2026-04-21",
                "type": "MULTI",
                "isPrimary": true,
                "description": "2026학년도 1학기 중간고사"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_COURSE_BULK = """
            {
              "success": true,
              "data": {
                "semester": "2026-1",
                "created": 120,
                "updated": 5,
                "deleted": 3
              }
            }
            """;

    public static final String ERROR_ADMIN_COURSE_BULK_CONFLICT =
            "{\"success\":false,\"message\":\"강의 bulk 처리 중 충돌이 발생했습니다.\",\"errorCode\":\"CONFLICT\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String SUCCESS_ADMIN_COURSE_DELETE = """
            {
              "success": true,
              "data": {
                "semester": "2026-1",
                "created": 0,
                "updated": 0,
                "deleted": 125
              }
            }
            """;

    public static final String ERROR_COURSE_NOT_FOUND =
            "{\"success\":false,\"message\":\"강의를 찾을 수 없습니다.\",\"errorCode\":\"COURSE_NOT_FOUND\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_TIMETABLE_CONFLICT =
            "{\"success\":false,\"message\":\"시간이 겹치는 강의가 이미 시간표에 있습니다.\",\"errorCode\":\"TIMETABLE_CONFLICT\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_COURSE_ALREADY_IN_TIMETABLE =
            "{\"success\":false,\"message\":\"이미 시간표에 추가된 강의입니다.\",\"errorCode\":\"COURSE_ALREADY_IN_TIMETABLE\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_ACADEMIC_SCHEDULE_NOT_FOUND =
            "{\"success\":false,\"message\":\"학사 일정을 찾을 수 없습니다.\",\"errorCode\":\"ACADEMIC_SCHEDULE_NOT_FOUND\",\"timestamp\":\"2026-03-07T14:00:00\"}";
}
