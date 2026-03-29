package com.skuri.skuri_backend.domain.notification.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.notification.dto.request.DeleteFcmTokenRequest;
import com.skuri.skuri_backend.domain.notification.dto.request.RegisterFcmTokenRequest;
import com.skuri.skuri_backend.domain.notification.service.FcmTokenService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiNotificationExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members/me/fcm-tokens")
@Tag(name = "FCM Token API", description = "내 FCM 토큰 등록/해제 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping
    @Operation(
            summary = "FCM 토큰 등록",
            description = "현재 사용자의 디바이스 FCM 토큰을 등록하거나 갱신합니다. appVersion은 optional이며, 신규 토큰 등록 시 미전송하면 null로 저장되고 기존 토큰 재등록 시 null이면 기존 값을 유지합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "등록 성공",
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
                    description = "접근 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_FORBIDDEN)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "유효성 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "FCM 토큰 등록 요청",
            content = @Content(
                    schema = @Schema(implementation = RegisterFcmTokenRequest.class),
                    examples = {
                            @ExampleObject(name = "with_app_version", value = OpenApiNotificationExamples.REQUEST_REGISTER_FCM_TOKEN),
                            @ExampleObject(
                                    name = "without_app_version",
                                    value = OpenApiNotificationExamples.REQUEST_REGISTER_FCM_TOKEN_WITHOUT_APP_VERSION
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<Void>> register(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody RegisterFcmTokenRequest request
    ) {
        fcmTokenService.register(
                requireAuthenticatedMember(authenticatedMember).uid(),
                request.token(),
                request.platform(),
                request.appVersion()
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping
    @Operation(summary = "FCM 토큰 삭제", description = "현재 사용자의 디바이스 FCM 토큰을 해제합니다.")
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
                    responseCode = "422",
                    description = "유효성 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "FCM 토큰 삭제 요청",
            content = @Content(
                    schema = @Schema(implementation = DeleteFcmTokenRequest.class),
                    examples = @ExampleObject(value = "{\"token\":\"dXZlbnQ6ZmNtLXRva2Vu\"}")
            )
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody DeleteFcmTokenRequest request
    ) {
        fcmTokenService.delete(requireAuthenticatedMember(authenticatedMember).uid(), request.token());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
