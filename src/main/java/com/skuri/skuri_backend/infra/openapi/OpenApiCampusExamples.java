package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiCampusExamples {

    private OpenApiCampusExamples() {
    }

    public static final String SUCCESS_CAMPUS_BANNERS_PUBLIC_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "campus_banner_uuid_1",
                  "badgeLabel": "택시 파티",
                  "titleLabel": "택시 동승 매칭",
                  "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                  "buttonLabel": "파티 찾기",
                  "paletteKey": "GREEN",
                  "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                  "actionType": "IN_APP",
                  "actionTarget": "TAXI_MAIN",
                  "actionParams": null,
                  "actionUrl": null
                },
                {
                  "id": "campus_banner_uuid_2",
                  "badgeLabel": "공지사항",
                  "titleLabel": "학교 공지사항",
                  "descriptionLabel": "중요한 학교 소식을 놓치지 말고 확인하세요",
                  "buttonLabel": "공지 보기",
                  "paletteKey": "BLUE",
                  "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-2.jpg",
                  "actionType": "IN_APP",
                  "actionTarget": "NOTICE_MAIN",
                  "actionParams": null,
                  "actionUrl": null
                },
                {
                  "id": "campus_banner_uuid_3",
                  "badgeLabel": "시간표",
                  "titleLabel": "나의 시간표",
                  "descriptionLabel": "오늘 수업 일정을 한눈에 확인하세요",
                  "buttonLabel": "시간표 보기",
                  "paletteKey": "PURPLE",
                  "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-3.jpg",
                  "actionType": "IN_APP",
                  "actionTarget": "TIMETABLE_DETAIL",
                  "actionParams": {
                    "initialView": "all"
                  },
                  "actionUrl": null
                }
              ]
            }
            """;

    public static final String REQUEST_ADMIN_CAMPUS_BANNER_CREATE = """
            {
              "badgeLabel": "택시 파티",
              "titleLabel": "택시 동승 매칭",
              "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
              "buttonLabel": "파티 찾기",
              "paletteKey": "GREEN",
              "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
              "actionType": "IN_APP",
              "actionTarget": "TAXI_MAIN",
              "actionParams": null,
              "actionUrl": null,
              "isActive": true,
              "displayStartAt": "2026-03-25T00:00:00",
              "displayEndAt": null
            }
            """;

    public static final String SUCCESS_ADMIN_CAMPUS_BANNER_CREATE = """
            {
              "success": true,
              "data": {
                "id": "campus_banner_uuid_1",
                "badgeLabel": "택시 파티",
                "titleLabel": "택시 동승 매칭",
                "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                "buttonLabel": "파티 찾기",
                "paletteKey": "GREEN",
                "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                "actionType": "IN_APP",
                "actionTarget": "TAXI_MAIN",
                "actionParams": null,
                "actionUrl": null,
                "isActive": true,
                "displayStartAt": "2026-03-25T00:00:00",
                "displayEndAt": null,
                "displayOrder": 1,
                "createdAt": "2026-03-25T10:00:00",
                "updatedAt": "2026-03-25T10:00:00"
              }
            }
            """;

    public static final String REQUEST_ADMIN_CAMPUS_BANNER_UPDATE = """
            {
              "buttonLabel": "공지 보기",
              "paletteKey": "BLUE",
              "actionType": "IN_APP",
              "actionTarget": "NOTICE_MAIN",
              "actionParams": null,
              "actionUrl": null,
              "isActive": true,
              "displayEndAt": "2026-04-30T23:59:59"
            }
            """;

    public static final String SUCCESS_ADMIN_CAMPUS_BANNER_UPDATE = """
            {
              "success": true,
              "data": {
                "id": "campus_banner_uuid_1",
                "badgeLabel": "택시 파티",
                "titleLabel": "택시 동승 매칭",
                "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                "buttonLabel": "공지 보기",
                "paletteKey": "BLUE",
                "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                "actionType": "IN_APP",
                "actionTarget": "NOTICE_MAIN",
                "actionParams": null,
                "actionUrl": null,
                "isActive": true,
                "displayStartAt": "2026-03-25T00:00:00",
                "displayEndAt": "2026-04-30T23:59:59",
                "displayOrder": 1,
                "createdAt": "2026-03-25T10:00:00",
                "updatedAt": "2026-03-25T10:30:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CAMPUS_BANNERS_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "campus_banner_uuid_1",
                  "badgeLabel": "택시 파티",
                  "titleLabel": "택시 동승 매칭",
                  "descriptionLabel": "같은 방향 가는 학생과 택시비를 함께 나눠요",
                  "buttonLabel": "파티 찾기",
                  "paletteKey": "GREEN",
                  "imageUrl": "https://cdn.skuri.app/uploads/campus-banners/2026/03/25/banner-1.jpg",
                  "actionType": "IN_APP",
                  "actionTarget": "TAXI_MAIN",
                  "actionParams": null,
                  "actionUrl": null,
                  "isActive": true,
                  "displayStartAt": "2026-03-25T00:00:00",
                  "displayEndAt": null,
                  "displayOrder": 1,
                  "createdAt": "2026-03-25T10:00:00",
                  "updatedAt": "2026-03-25T10:00:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_CAMPUS_BANNER_DETAIL = SUCCESS_ADMIN_CAMPUS_BANNER_CREATE;

    public static final String REQUEST_ADMIN_CAMPUS_BANNER_REORDER = """
            {
              "bannerIds": [
                "campus_banner_uuid_2",
                "campus_banner_uuid_1",
                "campus_banner_uuid_3"
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_CAMPUS_BANNER_REORDER = """
            {
              "success": true,
              "data": [
                {
                  "id": "campus_banner_uuid_2",
                  "displayOrder": 1
                },
                {
                  "id": "campus_banner_uuid_1",
                  "displayOrder": 2
                },
                {
                  "id": "campus_banner_uuid_3",
                  "displayOrder": 3
                }
              ]
            }
            """;

    public static final String ERROR_CAMPUS_BANNER_NOT_FOUND =
            "{\"success\":false,\"message\":\"캠퍼스 홈 배너를 찾을 수 없습니다.\",\"errorCode\":\"CAMPUS_BANNER_NOT_FOUND\",\"timestamp\":\"2026-03-25T12:00:00\"}";
}
