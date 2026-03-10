package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiImageExamples {

    private OpenApiImageExamples() {
    }

    public static final String SUCCESS_IMAGE_UPLOAD = """
            {
              "success": true,
              "data": {
                "url": "https://cdn.skuri.app/uploads/posts/2026/03/10/4f3ec1a0.jpg",
                "thumbUrl": "https://cdn.skuri.app/uploads/posts/2026/03/10/4f3ec1a0_thumb.jpg",
                "width": 800,
                "height": 600,
                "size": 245123,
                "mime": "image/jpeg"
              }
            }
            """;

    public static final String ERROR_IMAGE_TOO_LARGE = """
            {
              "success": false,
              "errorCode": "IMAGE_TOO_LARGE",
              "message": "이미지 파일은 최대 10MB까지 업로드할 수 있습니다.",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;

    public static final String ERROR_IMAGE_EMPTY_FILE = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "file은 비어 있을 수 없습니다.",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;

    public static final String ERROR_IMAGE_INVALID_CONTEXT = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "지원하지 않는 context입니다: UNKNOWN_CONTEXT",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;

    public static final String ERROR_IMAGE_INVALID_FORMAT = """
            {
              "success": false,
              "errorCode": "IMAGE_INVALID_FORMAT",
              "message": "지원하지 않는 이미지 형식입니다. JPEG, PNG, WebP만 업로드할 수 있습니다.",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;

    public static final String ERROR_IMAGE_UPLOAD_FAILED = """
            {
              "success": false,
              "errorCode": "IMAGE_UPLOAD_FAILED",
              "message": "이미지 업로드에 실패했습니다.",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;

    public static final String ERROR_IMAGE_DIMENSIONS_EXCEEDED = """
            {
              "success": false,
              "errorCode": "IMAGE_DIMENSIONS_EXCEEDED",
              "message": "이미지 해상도가 허용 범위를 초과했습니다.",
              "timestamp": "2026-03-10T12:00:00"
            }
            """;
}
