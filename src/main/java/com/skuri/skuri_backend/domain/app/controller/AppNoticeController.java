package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.infra.openapi.OpenApiAppExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/app-notices")
@Tag(name = "App API", description = "앱 버전/앱 공지 공개 API")
public class AppNoticeController {

    @GetMapping
    @Operation(
            summary = "앱 공지 목록 조회",
            description = "로그인 전에도 호출 가능한 공개 API입니다.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiAppExamples.SUCCESS_APP_NOTICES)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAppNotices() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}
