package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "학식 메뉴 응답. 기존 menus와 프론트용 구조화 menuEntries를 함께 제공합니다.")
public record CafeteriaMenuResponse(
        @Schema(description = "주차 ID", example = "2026-W06")
        String weekId,

        @Schema(description = "주 시작일", example = "2026-02-03")
        LocalDate weekStart,

        @Schema(description = "주 종료일", example = "2026-02-07")
        LocalDate weekEnd,

        @Schema(
                description = "날짜별 식당 메뉴 맵",
                example = """
                        {
                          "2026-02-03": {
                            "rollNoodles": ["우동", "김밥"],
                            "theBab": ["돈까스", "된장찌개"],
                            "fryRice": ["볶음밥", "짜장면"]
                          }
                        }
                        """
        )
        Map<String, Map<String, List<String>>> menus,

        @Schema(
                description = "학식 카테고리 정의 목록",
                example = """
                        [
                          {
                            "code": "rollNoodles",
                            "label": "Roll & Noodles"
                          },
                          {
                            "code": "theBab",
                            "label": "The bab"
                          },
                          {
                            "code": "fryRice",
                            "label": "Fry & Rice"
                          }
                        ]
                        """
        )
        List<CafeteriaMenuCategoryResponse> categories,

        @Schema(
                description = "날짜/카테고리별 구조화 메뉴 메타데이터",
                example = """
                        {
                          "2026-02-03": {
                            "rollNoodles": [
                              {
                                "id": "2026-W06.rollNoodles.76af7fdde6f4de15",
                                "title": "존슨부대찌개",
                                "badges": [
                                  {
                                    "code": "TAKEOUT",
                                    "label": "테이크아웃"
                                  }
                                ],
                                "likeCount": 178,
                                "dislikeCount": 22,
                                "myReaction": "LIKE"
                              }
                            ],
                            "theBab": [],
                            "fryRice": []
                          }
                        }
                        """
        )
        Map<String, Map<String, List<CafeteriaMenuEntryResponse>>> menuEntries
) {
}
