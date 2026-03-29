package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.support.dto.request.UpsertCafeteriaMenuReactionRequest;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuReactionResponse;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuReactionService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiSupportSchemas;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMemberSupport.requireAuthenticatedMember;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/cafeteria-menu-reactions")
@Tag(name = "Support Cafeteria Reaction API", description = "학식 메뉴 반응 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class CafeteriaMenuReactionController {

    private final CafeteriaMenuReactionService cafeteriaMenuReactionService;

    @PutMapping("/{menuId}")
    @Operation(
            summary = "학식 메뉴 반응 저장",
            description = "주간 기준 메뉴 ID에 대해 좋아요/싫어요를 등록, 전환, 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiSupportSchemas.CafeteriaMenuReactionApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "like", value = OpenApiSupportExamples.SUCCESS_CAFETERIA_MENU_REACTION_LIKE),
                                    @ExampleObject(name = "cancel", value = OpenApiSupportExamples.SUCCESS_CAFETERIA_MENU_REACTION_CANCEL)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 menuId 또는 요청 형식",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
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
                    description = "이메일 도메인 제한",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "학식 메뉴 또는 메뉴 항목 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "cafeteria_menu_not_found", value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_NOT_FOUND),
                                    @ExampleObject(name = "cafeteria_menu_entry_not_found", value = OpenApiSupportExamples.ERROR_CAFETERIA_MENU_ENTRY_NOT_FOUND)
                            }
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "반응 저장 요청. reaction이 null이면 기존 반응을 취소합니다.",
            content = @Content(
                    schema = @Schema(implementation = UpsertCafeteriaMenuReactionRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "like",
                                    value = """
                                            {
                                              "reaction": "LIKE"
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "cancel",
                                    value = """
                                            {
                                              "reaction": null
                                            }
                                            """
                            )
                    }
            )
    )
    public ResponseEntity<ApiResponse<CafeteriaMenuReactionResponse>> upsertReaction(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Parameter(description = "주간 기준 메뉴 ID", example = "2026-W08.rollNoodles.c4973864db4f8815")
            @PathVariable String menuId,
            @Valid @RequestBody UpsertCafeteriaMenuReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                cafeteriaMenuReactionService.upsertReaction(
                        requireAuthenticatedMember(authenticatedMember).uid(),
                        menuId,
                        request
                )
        ));
    }
}
