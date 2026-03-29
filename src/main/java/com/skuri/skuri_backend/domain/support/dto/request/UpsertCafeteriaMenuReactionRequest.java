package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학식 메뉴 반응 저장 요청. reaction=null 이면 기존 반응을 취소합니다.")
public record UpsertCafeteriaMenuReactionRequest(
        @Schema(description = "저장할 최종 반응 상태", nullable = true, implementation = CafeteriaMenuReactionType.class)
        CafeteriaMenuReactionType reaction
) {
}
