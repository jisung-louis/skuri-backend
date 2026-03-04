package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartySseService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiTaxiPartyExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(
                                            name = "stream_full",
                                            summary = "파티 SSE 전체 이벤트 흐름 예시",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_STREAM_FULL
                                    ),
                                    @ExampleObject(
                                            name = "snapshot",
                                            summary = "초기 SNAPSHOT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_SNAPSHOT
                                    ),
                                    @ExampleObject(
                                            name = "party_created",
                                            summary = "PARTY_CREATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_CREATED
                                    ),
                                    @ExampleObject(
                                            name = "party_updated",
                                            summary = "PARTY_UPDATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_UPDATED
                                    ),
                                    @ExampleObject(
                                            name = "party_status_changed",
                                            summary = "PARTY_STATUS_CHANGED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_STATUS_CHANGED
                                    ),
                                    @ExampleObject(
                                            name = "party_member_joined",
                                            summary = "PARTY_MEMBER_JOINED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_MEMBER_JOINED
                                    ),
                                    @ExampleObject(
                                            name = "party_member_left",
                                            summary = "PARTY_MEMBER_LEFT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_MEMBER_LEFT
                                    ),
                                    @ExampleObject(
                                            name = "party_deleted",
                                            summary = "PARTY_DELETED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_DELETED
                                    ),
                                    @ExampleObject(
                                            name = "heartbeat",
                                            summary = "HEARTBEAT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTIES_HEARTBEAT
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            )
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(
                                            name = "stream_full",
                                            summary = "동승요청 SSE 전체 이벤트 흐름 예시",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTY_JOIN_REQUESTS_STREAM_FULL
                                    ),
                                    @ExampleObject(
                                            name = "snapshot",
                                            summary = "초기 SNAPSHOT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTY_JOIN_REQUESTS_SNAPSHOT
                                    ),
                                    @ExampleObject(
                                            name = "join_request_created",
                                            summary = "JOIN_REQUEST_CREATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTY_JOIN_REQUESTS_CREATED
                                    ),
                                    @ExampleObject(
                                            name = "join_request_updated",
                                            summary = "JOIN_REQUEST_UPDATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTY_JOIN_REQUESTS_UPDATED
                                    ),
                                    @ExampleObject(
                                            name = "heartbeat",
                                            summary = "HEARTBEAT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_PARTY_JOIN_REQUESTS_HEARTBEAT
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "리더 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_NOT_PARTY_LEADER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파티 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiTaxiPartyExamples.ERROR_PARTY_NOT_FOUND)
                    )
            )
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(
                                            name = "stream_full",
                                            summary = "내 동승요청 SSE 전체 이벤트 흐름 예시",
                                            value = OpenApiTaxiPartyExamples.SSE_MY_JOIN_REQUESTS_STREAM_FULL
                                    ),
                                    @ExampleObject(
                                            name = "snapshot",
                                            summary = "초기 SNAPSHOT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_MY_JOIN_REQUESTS_SNAPSHOT
                                    ),
                                    @ExampleObject(
                                            name = "my_join_request_created",
                                            summary = "MY_JOIN_REQUEST_CREATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_MY_JOIN_REQUESTS_CREATED
                                    ),
                                    @ExampleObject(
                                            name = "my_join_request_updated",
                                            summary = "MY_JOIN_REQUEST_UPDATED 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_MY_JOIN_REQUESTS_UPDATED
                                    ),
                                    @ExampleObject(
                                            name = "heartbeat",
                                            summary = "HEARTBEAT 이벤트",
                                            value = OpenApiTaxiPartyExamples.SSE_MY_JOIN_REQUESTS_HEARTBEAT
                                    )
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 상태 파라미터 형식 오류",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            )
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
