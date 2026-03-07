package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "학식 메뉴 응답")
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
        Map<String, Map<String, List<String>>> menus
) {
}
