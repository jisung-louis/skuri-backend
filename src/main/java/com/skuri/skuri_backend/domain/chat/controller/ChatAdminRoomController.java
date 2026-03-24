package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.chat.dto.request.AdminCreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.infra.admin.audit.AdminAudit;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditActions;
import com.skuri.skuri_backend.infra.admin.audit.AdminAuditTargetTypes;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiChatSchemas;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/admin/chat-rooms")
@Tag(name = "Chat Admin API", description = "관리자 공개 채팅방 관리 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
@AdminApiAccess
public class ChatAdminRoomController {

    private final ChatAdminService chatAdminService;

    @PostMapping
    @Operation(summary = "공개 채팅방 생성(관리자)", description = "관리자가 공개 채팅방을 생성합니다. PARTY 타입은 생성할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiChatSchemas.AdminCreateChatRoomApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_CREATE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비즈니스 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_type_not_allowed", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_PARTY_TYPE_NOT_ALLOWED),
                                    @ExampleObject(name = "public_only", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_PUBLIC_ONLY)
                            }
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "공개 채팅방 생성 요청",
            content = @Content(
                    schema = @Schema(implementation = AdminCreateChatRoomRequest.class),
                    examples = @ExampleObject(
                            value = "{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"description\":\"성결대학교 학생들의 소통 공간\",\"isPublic\":true}"
                    )
            )
    )
    @AdminAudit(
            action = AdminAuditActions.CHAT_ROOM_CREATED,
            targetType = AdminAuditTargetTypes.CHAT_ROOM,
            targetId = "#responseBody['data']['id']",
            after = "@adminAuditSnapshots.chatRoom(#responseBody['data']['id'])"
    )
    public ResponseEntity<ApiResponse<AdminCreateChatRoomResponse>> createPublicChatRoom(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody AdminCreateChatRoomRequest request
    ) {
        AdminCreateChatRoomResponse response = chatAdminService.createPublicChatRoom(
                requireAuthenticatedMember(authenticatedMember).uid(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{chatRoomId}")
    @Operation(summary = "공개 채팅방 삭제(관리자)", description = "관리자가 공개 채팅방을 삭제합니다. PARTY/비공개 채팅방은 삭제할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiChatExamples.SUCCESS_ADMIN_CHAT_ROOM_DELETE)
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
                    description = "관리자 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "비즈니스 검증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "party_type_not_allowed", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_DELETE_PARTY_NOT_ALLOWED),
                                    @ExampleObject(name = "public_only", value = OpenApiChatExamples.ERROR_ADMIN_CHAT_ROOM_DELETE_PUBLIC_ONLY)
                            }
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
    @AdminAudit(
            action = AdminAuditActions.CHAT_ROOM_DELETED,
            targetType = AdminAuditTargetTypes.CHAT_ROOM,
            targetId = "#chatRoomId",
            before = "@adminAuditSnapshots.chatRoom(#chatRoomId)"
    )
    public ResponseEntity<ApiResponse<Void>> deletePublicChatRoom(
            @PathVariable String chatRoomId
    ) {
        chatAdminService.deletePublicChatRoom(chatRoomId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
