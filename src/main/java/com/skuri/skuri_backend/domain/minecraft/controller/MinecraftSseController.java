package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftPublicSseService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiMinecraftExamples;
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
@Tag(name = "Minecraft API", description = "마인크래프트 상세 화면 실시간 상태 SSE")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class MinecraftSseController {

    private final MinecraftPublicSseService minecraftPublicSseService;

    @GetMapping(value = "/minecraft", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "마인크래프트 상태 SSE 구독", description = "서버 상태와 플레이어 목록의 실시간 변경을 SSE로 구독합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(name = "stream_full", value = OpenApiMinecraftExamples.SSE_MINECRAFT_STREAM_FULL),
                                    @ExampleObject(name = "server_state_snapshot", value = OpenApiMinecraftExamples.SSE_MINECRAFT_SERVER_STATE_SNAPSHOT),
                                    @ExampleObject(name = "players_snapshot", value = OpenApiMinecraftExamples.SSE_MINECRAFT_PLAYERS_SNAPSHOT),
                                    @ExampleObject(name = "heartbeat", value = OpenApiMinecraftExamples.SSE_MINECRAFT_HEARTBEAT)
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
        return minecraftPublicSseService.subscribe(requireAuthenticatedMember(authenticatedMember).uid());
    }
}
