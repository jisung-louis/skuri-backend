package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestAcceptResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestListItemResponse;
import com.skuri.skuri_backend.domain.taxiparty.dto.response.JoinRequestResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "TaxiParty API", description = "동승 요청 상태 전이 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class JoinRequestController {

    private final TaxiPartyService taxiPartyService;

    @PatchMapping("/join-requests/{id}/accept")
    @Operation(summary = "동승 요청 수락", description = "리더가 동승 요청을 수락합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수락 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리더 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리됨/파티 상태 불가/동시성 충돌")
    })
    public ResponseEntity<ApiResponse<JoinRequestAcceptResponse>> accept(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String requestId
    ) {
        JoinRequestAcceptResponse response = taxiPartyService.acceptJoinRequest(requireAuthenticatedMember(authenticatedMember).uid(), requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/join-requests/{id}/decline")
    @Operation(summary = "동승 요청 거절", description = "리더가 동승 요청을 거절합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "거절 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리더 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리됨")
    })
    public ResponseEntity<ApiResponse<JoinRequestResponse>> decline(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String requestId
    ) {
        JoinRequestResponse response = taxiPartyService.declineJoinRequest(requireAuthenticatedMember(authenticatedMember).uid(), requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/join-requests/{id}/cancel")
    @Operation(summary = "동승 요청 취소", description = "요청자가 자신의 동승 요청을 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "요청자 본인 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "요청 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 처리됨")
    })
    public ResponseEntity<ApiResponse<JoinRequestResponse>> cancel(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String requestId
    ) {
        JoinRequestResponse response = taxiPartyService.cancelJoinRequest(requireAuthenticatedMember(authenticatedMember).uid(), requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/members/me/join-requests")
    @Operation(summary = "내 동승 요청 목록", description = "내가 보낸 동승 요청 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 상태 파라미터 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<List<JoinRequestListItemResponse>>> getMyJoinRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "status", required = false) JoinRequestStatus status
    ) {
        List<JoinRequestListItemResponse> response = taxiPartyService.getMyJoinRequests(
                requireAuthenticatedMember(authenticatedMember).uid(),
                status
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
