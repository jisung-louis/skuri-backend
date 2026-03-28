package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members/me/app-notices")
@Tag(name = "App Notice API", description = "내 앱 공지 읽음 상태 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MemberAppNoticeController {

    private final AppNoticeService appNoticeService;

    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 앱 공지 수 조회", description = "현재 사용자의 미읽음 앱 공지 수를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeUnreadCountApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_UNREAD_COUNT)
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
    public ResponseEntity<ApiResponse<AppNoticeUnreadCountResponse>> getUnreadCount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.getUnreadCount(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PostMapping("/{appNoticeId}/read")
    @Operation(summary = "앱 공지 읽음 처리", description = "현재 사용자의 앱 공지를 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "읽음 처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiAppSchemas.AppNoticeReadApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICE_MARK_READ)
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
                    responseCode = "404",
                    description = "앱 공지 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "app_notice_not_found", value = OpenApiAppExamples.ERROR_APP_NOTICE_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<AppNoticeReadResponse>> markRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "앱 공지 ID", example = "app_notice_uuid")
            @PathVariable String appNoticeId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                appNoticeService.markRead(requireAuthenticatedMember(authenticatedMember).uid(), appNoticeId)
        ));
    }
}
