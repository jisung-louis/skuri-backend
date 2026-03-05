package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatRoomReadRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.UpdateChatRoomSettingsRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/chat-rooms")
@Tag(name = "Chat API", description = "채팅방 조회/설정/읽음 처리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ChatRoomController {

    private final ChatService chatService;

    @GetMapping
    @Operation(summary = "채팅방 목록 조회", description = "타입/참여 여부 조건으로 채팅방 목록을 조회합니다. (공개 채팅방 + 내가 참여한 비공개 채팅방)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_LIST)
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
    public ResponseEntity<ApiResponse<List<ChatRoomSummaryResponse>>> getChatRooms(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @RequestParam(name = "type", required = false) ChatRoomType type,
            @RequestParam(name = "joined", required = false) Boolean joined
    ) {
        List<ChatRoomSummaryResponse> response = chatService.getChatRooms(
                requireAuthenticatedMember(authenticatedMember).uid(),
                type,
                joined
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "채팅방 상세 조회", description = "채팅방 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_ROOM_DETAIL)
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
                    description = "비공개 채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomDetailResponse>> getChatRoom(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId
    ) {
        ChatRoomDetailResponse response = chatService.getChatRoomDetail(requireAuthenticatedMember(authenticatedMember).uid(), chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "채팅 메시지 조회", description = "메시지를 createdAt DESC, id DESC 정렬로 커서 기반 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_MESSAGES_PAGE)
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
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "cursor_pair_required", value = OpenApiChatExamples.ERROR_VALIDATION_CURSOR_PAIR),
                                    @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                            }
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatMessagePageResponse>> getMessages(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @RequestParam(name = "cursorCreatedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime cursorCreatedAt,
            @RequestParam(name = "cursorId", required = false) String cursorId,
            @RequestParam(name = "size", required = false) Integer size
    ) {
        ChatMessagePageResponse response = chatService.getMessages(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                cursorCreatedAt,
                cursorId,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "읽음 처리", description = "lastReadAt 단조 증가를 보장하며 읽음 시각을 갱신합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_READ_UPDATE)
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
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatReadUpdateResponse>> markAsRead(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @Valid @RequestBody UpdateChatRoomReadRequest request
    ) {
        ChatReadUpdateResponse response = chatService.markAsRead(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                request.lastReadAt()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/settings")
    @Operation(summary = "채팅방 설정 수정", description = "음소거 등 채팅방 사용자 설정을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_CHAT_SETTINGS_UPDATE)
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
                    description = "채팅방 멤버가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "not_chat_room_member", value = OpenApiChatExamples.ERROR_NOT_CHAT_ROOM_MEMBER)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "chat_room_not_found", value = OpenApiChatExamples.ERROR_CHAT_ROOM_NOT_FOUND)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "요청 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "bean_validation", value = OpenApiCommonExamples.ERROR_VALIDATION)
                    )
            )
    })
    public ResponseEntity<ApiResponse<ChatRoomSettingsResponse>> updateSettings(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @PathVariable("id") String chatRoomId,
            @Valid @RequestBody UpdateChatRoomSettingsRequest request
    ) {
        ChatRoomSettingsResponse response = chatService.updateSettings(
                requireAuthenticatedMember(authenticatedMember).uid(),
                chatRoomId,
                request.muted()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
