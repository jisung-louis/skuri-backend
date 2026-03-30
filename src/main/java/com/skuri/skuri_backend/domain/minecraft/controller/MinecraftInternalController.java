package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftInternalChatMessageRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftOnlinePlayersUpsertRequest;
import com.skuri.skuri_backend.domain.minecraft.dto.request.MinecraftServerStateUpsertRequest;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftBridgeService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiMinecraftExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/minecraft")
@Tag(name = "Minecraft Internal API", description = "플러그인 전용 마인크래프트 internal bridge API")
public class MinecraftInternalController {

    private final MinecraftBridgeService minecraftBridgeService;
    private final com.skuri.skuri_backend.domain.minecraft.service.MinecraftInternalSseService minecraftInternalSseService;

    @PostMapping("/chat/messages")
    @Operation(summary = "마인크래프트 채팅/시스템 메시지 수신", description = "플러그인이 게임 내 채팅 또는 시스템 메시지를 Spring 서버로 전달합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MinecraftInternalChatMessageRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.REQUEST_INTERNAL_CHAT_MESSAGE)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "shared secret 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "minecraft_secret_invalid", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SECRET_INVALID)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> postChatMessage(
            @Parameter(hidden = true) @RequestHeader(MinecraftInternalSecretFilter.HEADER_NAME) String ignoredSecret,
            @Valid @RequestBody MinecraftInternalChatMessageRequest request
    ) {
        minecraftBridgeService.handleIncomingChatMessage(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/server-state")
    @Operation(summary = "마인크래프트 서버 상태 upsert", description = "플러그인이 서버 heartbeat와 상태 정보를 갱신합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MinecraftServerStateUpsertRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.REQUEST_INTERNAL_SERVER_STATE)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "shared secret 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "minecraft_secret_invalid", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SECRET_INVALID)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> putServerState(
            @Parameter(hidden = true) @RequestHeader(MinecraftInternalSecretFilter.HEADER_NAME) String ignoredSecret,
            @Valid @RequestBody MinecraftServerStateUpsertRequest request
    ) {
        minecraftBridgeService.upsertServerState(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/online-players")
    @Operation(summary = "마인크래프트 온라인 플레이어 스냅샷 upsert", description = "플러그인이 현재 온라인 플레이어 스냅샷을 갱신합니다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MinecraftOnlinePlayersUpsertRequest.class),
                    examples = @ExampleObject(name = "default", value = OpenApiMinecraftExamples.REQUEST_INTERNAL_ONLINE_PLAYERS)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.SUCCESS_NULL)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "shared secret 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "minecraft_secret_invalid", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SECRET_INVALID)
                    )
            )
    })
    public ResponseEntity<ApiResponse<Void>> putOnlinePlayers(
            @Parameter(hidden = true) @RequestHeader(MinecraftInternalSecretFilter.HEADER_NAME) String ignoredSecret,
            @Valid @RequestBody MinecraftOnlinePlayersUpsertRequest request
    ) {
        minecraftBridgeService.replaceOnlinePlayers(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/stream")
    @Operation(summary = "플러그인 outbound SSE 구독", description = "앱에서 보낸 마인크래프트 채팅과 화이트리스트 변경을 SSE로 구독합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 연결 성공",
                    content = @Content(
                            mediaType = "text/event-stream",
                            schema = @Schema(type = "string"),
                            examples = {
                                    @ExampleObject(name = "stream_full", value = OpenApiMinecraftExamples.SSE_INTERNAL_MINECRAFT_STREAM_FULL),
                                    @ExampleObject(name = "whitelist_snapshot", value = OpenApiMinecraftExamples.SSE_INTERNAL_MINECRAFT_WHITELIST_SNAPSHOT),
                                    @ExampleObject(name = "chat_from_app", value = OpenApiMinecraftExamples.SSE_INTERNAL_MINECRAFT_CHAT_FROM_APP),
                                    @ExampleObject(name = "heartbeat", value = OpenApiMinecraftExamples.SSE_INTERNAL_MINECRAFT_HEARTBEAT)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "shared secret 불일치",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "minecraft_secret_invalid", value = OpenApiMinecraftExamples.ERROR_MINECRAFT_SECRET_INVALID)
                    )
            )
    })
    public SseEmitter stream(
            @Parameter(hidden = true) @RequestHeader(MinecraftInternalSecretFilter.HEADER_NAME) String ignoredSecret,
            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId
    ) {
        return minecraftInternalSseService.subscribe(lastEventId);
    }
}
