package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "학식 메뉴 등록 요청")
public record CreateCafeteriaMenuRequest(
        @NotBlank(message = "weekId는 필수입니다.")
        @Pattern(regexp = "^\\d{4}-W\\d{2}$", message = "weekId 형식은 yyyy-Www 이어야 합니다.")
        @Schema(description = "주차 ID", example = "2026-W08")
        String weekId,

        @NotNull(message = "weekStart는 필수입니다.")
        @Schema(description = "주 시작일", example = "2026-02-16")
        LocalDate weekStart,

        @NotNull(message = "weekEnd는 필수입니다.")
        @Schema(description = "주 종료일", example = "2026-02-20")
        LocalDate weekEnd,

        @NotEmpty(message = "menus는 비어 있을 수 없습니다.")
        @Schema(
                description = "날짜별 식당 메뉴 맵",
                example = """
                        {
                          "2026-02-16": {
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
