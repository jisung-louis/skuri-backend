package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftOverviewResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftReadService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/minecraft")
@Tag(name = "Minecraft API", description = "마인크래프트 상태/플레이어 조회 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MinecraftController {

    private final MinecraftReadService minecraftReadService;

    @GetMapping("/overview")
    @Operation(summary = "마인크래프트 서버 개요 조회", description = "서버 상태 카드와 채팅방 진입 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMinecraftSchemas.MinecraftOverviewApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.SUCCESS_MINECRAFT_OVERVIEW)
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
                    responseCode = "503",
                    description = "서버 상태 미수신",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "minecraft_server_unavailable", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SERVER_UNAVAILABLE)
                    )
            )
    })
    public ResponseEntity<ApiResponse<MinecraftOverviewResponse>> getOverview(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                minecraftReadService.getOverview(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }

    @GetMapping("/players")
    @Operation(summary = "마인크래프트 플레이어 목록 조회", description = "화이트리스트 플레이어 목록과 현재 온라인 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiMinecraftSchemas.MinecraftPlayerListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.SUCCESS_MINECRAFT_PLAYERS)
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
    public ResponseEntity<ApiResponse<List<MinecraftPlayerResponse>>> getPlayers(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                minecraftReadService.getPlayers(requireAuthenticatedMember(authenticatedMember).uid())
        ));
    }
}
