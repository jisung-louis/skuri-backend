package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "파티 생성 요청")
public record CreatePartyRequest(
        @Schema(description = "출발지")
        @NotNull(message = "departure는 필수입니다.")
        @Valid
        PartyLocationRequest departure,

        @Schema(description = "목적지")
        @NotNull(message = "destination은 필수입니다.")
        @Valid
        PartyLocationRequest destination,

        @Schema(description = "출발 시각", example = "2026-03-03T14:00:00")
        @NotNull(message = "departureTime은 필수입니다.")
        @Future(message = "departureTime은 현재 이후 시각이어야 합니다.")
        LocalDateTime departureTime,

        @Schema(description = "최대 인원", example = "4", minimum = "2", maximum = "7")
        @Min(value = 2, message = "maxMembers는 2 이상이어야 합니다.")
        @Max(value = 7, message = "maxMembers는 7 이하여야 합니다.")
        int maxMembers,

        @Schema(description = "태그 목록", example = "[\"빠른출발\",\"조용한분\"]", nullable = true)
        @Size(max = 10, message = "tags는 최대 10개까지 가능합니다.")
        List<@Size(max = 50, message = "tag는 50자 이하여야 합니다.") String> tags,

        @Schema(description = "상세 설명", example = "택시비 나눠요", nullable = true)
        @Size(max = 500, message = "detail은 500자 이하여야 합니다.")
        String detail
) {
}
