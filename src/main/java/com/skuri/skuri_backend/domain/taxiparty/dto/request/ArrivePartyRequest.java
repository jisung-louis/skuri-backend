package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "도착 처리 요청")
public record ArrivePartyRequest(
        @Schema(description = "총 택시비", example = "14000")
        @Min(value = 1, message = "taxiFare는 1원 이상이어야 합니다.")
        int taxiFare,
        @Schema(
                description = "정산 대상 non-leader 멤버 ID 목록. 리더는 자동으로 분모에 포함되며 별도 지정하지 않습니다.",
                example = "[\"member-2\",\"member-3\"]"
        )
        @NotEmpty(message = "settlementTargetMemberIds는 최소 1명 이상이어야 합니다.")
        List<@NotBlank(message = "settlementTargetMemberIds에는 빈 값이 올 수 없습니다.") String> settlementTargetMemberIds,
        @Valid
        @NotNull(message = "account는 필수입니다.")
        @Schema(description = "도착/정산 안내 메시지에 포함할 계좌 snapshot")
        SettlementAccountRequest account
) {

    @Schema(description = "도착/정산용 계좌 snapshot")
    public record SettlementAccountRequest(
            @Schema(description = "은행명", example = "카카오뱅크")
            @NotBlank(message = "account.bankName은 필수입니다.")
            @Size(max = 20, message = "account.bankName은 20자 이하여야 합니다.")
            String bankName,
            @Schema(description = "계좌번호", example = "3333-01-1234567")
            @NotBlank(message = "account.accountNumber는 필수입니다.")
            @Size(max = 30, message = "account.accountNumber는 30자 이하여야 합니다.")
            String accountNumber,
            @Schema(description = "예금주", example = "홍길동")
            @NotBlank(message = "account.accountHolder는 필수입니다.")
            @Size(max = 50, message = "account.accountHolder는 50자 이하여야 합니다.")
            String accountHolder,
            @Schema(description = "이름 일부 숨김 여부", example = "true", nullable = true)
            Boolean hideName
    ) {
    }
}
