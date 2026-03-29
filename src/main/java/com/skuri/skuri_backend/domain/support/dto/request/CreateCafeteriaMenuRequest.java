package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
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

        @Schema(
                description = "기존 날짜별 식당 메뉴 맵. menuEntries만으로도 등록할 수 있으며, 둘 다 전달하면 제목 목록이 일치해야 합니다. menus에서 비어 있는 카테고리를 생략한 경우는 menuEntries의 빈 배열과 동일하게 취급합니다.",
                example = """
                        {
                          "2026-02-16": {
                            "rollNoodles": ["우동", "김밥"],
                            "theBab": ["돈까스", "된장찌개"],
                            "fryRice": ["볶음밥", "짜장면"]
                          }
                        }
                        """,
                nullable = true
        )
        Map<String, Map<String, List<String>>> menus,

        @Schema(
                description = "프론트 렌더링용 구조화 메뉴 메타데이터. menus 없이 단독으로 전달할 수 있습니다. 같은 주 안에서 동일 카테고리의 동일 title은 날짜가 달라도 badges/likeCount/dislikeCount가 동일해야 하며, menus와 함께 비교할 때 빈 카테고리 배열은 생략과 동일하게 취급합니다. likeCount/dislikeCount 요청값은 deprecated이며 저장 시 무시됩니다.",
                example = """
                        {
                          "2026-02-16": {
                            "rollNoodles": [
                              {
                                "title": "존슨부대찌개",
                                "badges": [
                                  {
                                    "code": "TAKEOUT",
                                    "label": "테이크아웃"
                                  }
                                ]
                              }
                            ],
                            "theBab": [],
                            "fryRice": []
                          }
                        }
                        """,
                nullable = true
        )
        Map<String, Map<String, List<@Valid CafeteriaMenuEntryRequest>>> menuEntries
) {

    @AssertTrue(message = "menus 또는 menuEntries 중 하나는 비어 있을 수 없습니다.")
    @Schema(hidden = true)
    public boolean hasMenuPayload() {
        return hasValue(menus) || hasValue(menuEntries);
    }

    private static boolean hasValue(Map<?, ?> source) {
        return source != null && !source.isEmpty();
    }
}
