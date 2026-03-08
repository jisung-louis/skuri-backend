package com.skuri.skuri_backend.domain.notification.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiNotificationExamples;
import com.skuri.skuri_backend.domain.notification.service.NotificationSseService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/sse")
@Tag(name = "Notification SSE API", description = "알림 실시간 구독 SSE API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class NotificationSseController {

    private final NotificationSseService notificationSseService;

    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 SSE 구독", description = "내 알림 신규 도착과 미읽음 수 변경을 SSE로 구독합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(name = "stream_full", value = OpenApiNotificationExamples.SSE_NOTIFICATIONS_STREAM_FULL),
                                    @ExampleObject(name = "snapshot", value = OpenApiNotificationExamples.SSE_NOTIFICATIONS_SNAPSHOT),
                                    @ExampleObject(name = "notification", value = OpenApiNotificationExamples.SSE_NOTIFICATIONS_NOTIFICATION),
                                    @ExampleObject(name = "unread_count_changed", value = OpenApiNotificationExamples.SSE_NOTIFICATIONS_UNREAD_COUNT_CHANGED),
                                    @ExampleObject(name = "heartbeat", value = OpenApiNotificationExamples.SSE_NOTIFICATIONS_HEARTBEAT)
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
    public SseEmitter subscribe(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return notificationSseService.subscribe(requireAuthenticatedMember(authenticatedMember).uid());
    }
}
