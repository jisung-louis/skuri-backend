package com.skuri.skuri_backend.domain.image.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadContext;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadRequest;
import com.skuri.skuri_backend.domain.image.dto.response.ImageUploadResponse;
import com.skuri.skuri_backend.domain.image.service.ImageUploadService;
import com.skuri.skuri_backend.infra.openapi.OpenApiCommonExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiConfig;
import com.skuri.skuri_backend.infra.openapi.OpenApiImageExamples;
import com.skuri.skuri_backend.infra.openapi.OpenApiImageSchemas;
import com.skuri.skuri_backend.infra.openapi.OpenApiMemberExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/images")
@Tag(name = "Image API", description = "공통 이미지 업로드 API")
@SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME)
public class ImageController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ImageUploadService imageUploadService;

    public ImageController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 업로드", description = "이미지 파일을 업로드하고 원본/썸네일 URL과 메타데이터를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업로드 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = OpenApiImageSchemas.ImageUploadApiResponse.class),
                            examples = @ExampleObject(name = "default", value = OpenApiImageExamples.SUCCESS_IMAGE_UPLOAD)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 multipart 파라미터 또는 context",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "invalid_context", value = OpenApiImageExamples.ERROR_IMAGE_INVALID_CONTEXT),
                                    @ExampleObject(name = "empty_file", value = OpenApiImageExamples.ERROR_IMAGE_EMPTY_FILE),
                                    @ExampleObject(name = "invalid_request", value = OpenApiCommonExamples.ERROR_INVALID_REQUEST)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "unauthorized", value = OpenApiCommonExamples.ERROR_UNAUTHORIZED)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "이메일 도메인 제한/탈퇴 회원 차단/관리자 전용 context 차단",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = {
                                    @ExampleObject(name = "email_domain_restricted", value = OpenApiCommonExamples.ERROR_EMAIL_DOMAIN_RESTRICTED),
                                    @ExampleObject(name = "member_withdrawn", value = OpenApiMemberExamples.ERROR_MEMBER_WITHDRAWN),
                                    @ExampleObject(name = "admin_required", value = OpenApiCommonExamples.ERROR_ADMIN_REQUIRED)
                            }
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "413",
                    description = "파일 크기 초과",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "image_too_large", value = OpenApiImageExamples.ERROR_IMAGE_TOO_LARGE)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "415",
                    description = "지원하지 않는 이미지 형식",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "image_invalid_format", value = OpenApiImageExamples.ERROR_IMAGE_INVALID_FORMAT)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "이미지 해상도 초과",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    name = "image_dimensions_exceeded",
                                    value = OpenApiImageExamples.ERROR_IMAGE_DIMENSIONS_EXCEEDED
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "스토리지 저장 실패",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(name = "image_upload_failed", value = OpenApiImageExamples.ERROR_IMAGE_UPLOAD_FAILED)
                    )
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "multipart/form-data 업로드 요청",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = ImageUploadRequest.class),
                    examples = @ExampleObject(
                            name = "default",
                            value = """
                                    {
                                      "context": "POST_IMAGE",
                                      "file": "(binary)"
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @Parameter(hidden = true) Authentication authentication,
            @RequestPart("file") MultipartFile file,
            @RequestParam("context") String context
    ) {
        ImageUploadContext uploadContext = parseContext(context);
        ImageUploadResponse response = imageUploadService.upload(isAdmin(authentication), uploadContext, file);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private ImageUploadContext parseContext(String context) {
        try {
            return ImageUploadContext.from(context);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, e.getMessage());
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }
}
