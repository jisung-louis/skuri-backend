package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 법적 문서 삭제 응답")
public record LegalDocumentDeleteResponse(
        @Schema(description = "삭제된 문서 키", example = "termsOfUse")
        String id
) {
}
