package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.request.CreateMinecraftAccountRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftAccountService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMinecraftExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiMinecraftSchemas;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members/me/minecraft-accounts")
@Tag(name = "Minecraft API", description = "내 마인크래프트 계정 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MemberMinecraftAccountController {

    private final MinecraftAccountService minecraftAccountService;

    @GetMapping
    @Operation(summary = "내 마인크래프트 계정 목록 조회", description = "등록한 본인/친구 마인크래프트 계정 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMinecraftSchemas.MinecraftAccountListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.SUCCESS_MY_MINECRAFT_ACCOUNTS)
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
    public ResponseEntity<ApiResponse<List<MinecraftAccountResponse>>> getMyAccounts(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                minecraftAccountService.getMyAccounts(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @PostMapping
    @Operation(summary = "마인크래프트 계정 등록", description = "본인 또는 친구 마인크래프트 계정을 등록합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CreateMinecraftAccountRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.REQUEST_CREATE_MINECRAFT_ACCOUNT)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMinecraftSchemas.MinecraftAccountApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.SUCCESS_CREATE_MINECRAFT_ACCOUNT)
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
                    responseCode = "409",
                    description = "비즈니스 규칙 위반",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "limit_exceeded", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_ACCOUNT_LIMIT_EXCEEDED),
                                    @ExampleObject(name = "self_exists", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS),
                                    @ExampleObject(name = "friend_limit", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED),
                                    @ExampleObject(name = "parent_required", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_PARENT_ACCOUNT_REQUIRED),
                                    @ExampleObject(name = "duplicated", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_ACCOUNT_DUPLICATED)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MinecraftAccountResponse>> createAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody CreateMinecraftAccountRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                minecraftAccountService.createAccount(requireAuthenticatedMember(authenticatedMember).uid(), request)
        ));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "마인크래프트 계정 삭제", description = "등록한 마인크래프트 계정을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMinecraftSchemas.MinecraftAccountApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.SUCCESS_DELETE_MINECRAFT_ACCOUNT)
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
                    description = "대상 계정 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "부모 계정 삭제 불가",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "parent_delete_not_allowed", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MinecraftAccountResponse>> deleteAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "마인크래프트 계정 ID", example = "account-uuid")
            @PathVariable String accountId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                minecraftAccountService.deleteAccount(requireAuthenticatedMember(authenticatedMember).uid(), accountId)
        ));
    }
}
