package com.skuri.skuri_backend.domain.campus.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.domain.campus.dto.response.CampusBannerPublicResponse;
import com.skuri.skuri_backend.domain.campus.service.CampusBannerService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCampusExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiCampusSchemas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/campus-banners")
@Tag(name = "Campus Banner API", description = "캠퍼스 홈 배너 공개 API")
public class CampusBannerController {

    private final CampusBannerService campusBannerService;

    @GetMapping
    @Operation(
            summary = "캠퍼스 홈 배너 목록 조회",
            description = "로그인 전에도 호출 가능한 공개 API입니다.",
            security = @SecurityRequirement(name = "")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiCampusSchemas.CampusBannerPublicListApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiCampusExamples.SUCCESS_CAMPUS_BANNERS_PUBLIC_LIST)
                    )
            )
    })
    public ResponseEntity<ApiResponse<List<CampusBannerPublicResponse>>> getCampusBanners() {
        return ResponseEntity.ok(ApiResponse.success(campusBannerService.getPublicBanners()));
    }
}
