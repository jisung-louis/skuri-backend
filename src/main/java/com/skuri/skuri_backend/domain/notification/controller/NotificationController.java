package com.skuri.skuri_backend.domain.notification.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationListResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiNotificationExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiNotificationSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/notifications")
@Tag(name = "Notification API", description = "알림 인박스 조회/관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "페이지네이션과 unreadOnly 조건으로 내 알림 인박스를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNotificationSchemas.NotificationListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNotificationExamples.SUCCESS_NOTIFICATION_LIST)
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
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(
                requireAuthenticatedMember(authenticatedMember).uid(),
                unreadOnly,
                page,
                size
        )));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 알림 수 조회", description = "현재 미읽음 알림 수를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNotificationSchemas.NotificationUnreadCountApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNotificationExamples.SUCCESS_NOTIFICATION_UNREAD_COUNT)
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
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponse>> getUnreadCount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getUnreadCount(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "단일 알림을 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNotificationSchemas.NotificationApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNotificationExamples.SUCCESS_NOTIFICATION_READ)
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
                    description = "다른 사용자의 알림 접근",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_notification_owner", value = OpenApiNotificationExamples.ERROR_NOT_NOTIFICATION_OWNER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "알림 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notification_not_found", value = OpenApiNotificationExamples.ERROR_NOTIFICATION_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "알림 ID", example = "notification-uuid")
            @PathVariable String notificationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(
                requireAuthenticatedMember(authenticatedMember).uid(),
                notificationId
        )));
    }

    @PostMapping("/read-all")
    @Operation(summary = "전체 읽음 처리", description = "현재 사용자의 미읽음 알림을 모두 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiNotificationSchemas.NotificationReadAllApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiNotificationExamples.SUCCESS_NOTIFICATION_READ_ALL)
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
    public ResponseEntity<ApiResponse<NotificationReadAllResponse>> markAllRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markAllRead(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "알림 삭제", description = "단일 알림을 인박스에서 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
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
                    description = "다른 사용자의 알림 접근",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_notification_owner", value = OpenApiNotificationExamples.ERROR_NOT_NOTIFICATION_OWNER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "알림 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "notification_not_found", value = OpenApiNotificationExamples.ERROR_NOTIFICATION_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "알림 ID", example = "notification-uuid")
            @PathVariable String notificationId
    ) {
        notificationService.delete(requireAuthenticatedMember(authenticatedMember).uid(), notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
