package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartySseService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sse")
@Tag(name = "TaxiParty SSE API", description = "TaxiParty 파티/동승요청 실시간 구독 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class PartySseController {

    private final PartySseService partySseService;
    private final JoinRequestSseService joinRequestSseService;

    @GetMapping(value = "/parties", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "파티 SSE 구독", description = "파티 목록/상태 변경 이벤트를 SSE로 구독합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public SseEmitter subscribeParties(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return partySseService.subscribeParties(requireAuthenticatedMember(authenticatedMember).uid());
    }

    @GetMapping(value = "/parties/{partyId}/join-requests", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "파티 동승요청 SSE 구독", description = "파티 리더가 특정 파티의 동승요청 목록/상태 변경 이벤트를 SSE로 구독합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "리더 권한 없음"),
            @ApiResponse(responseCode = "404", description = "파티 없음")
    })
    public SseEmitter subscribePartyJoinRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("partyId") String partyId
    ) {
        return joinRequestSseService.subscribePartyJoinRequests(
                requireAuthenticatedMember(authenticatedMember).uid(),
                partyId
        );
    }

    @GetMapping(value = "/members/me/join-requests", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "내 동승요청 SSE 구독", description = "내 동승요청 목록/상태 변경 이벤트를 SSE로 구독합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "400", description = "요청 상태 파라미터 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public SseEmitter subscribeMyJoinRequests(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "status", required = false) JoinRequestStatus status
    ) {
        return joinRequestSseService.subscribeMyJoinRequests(
                requireAuthenticatedMember(authenticatedMember).uid(),
                status
        );
    }
}
