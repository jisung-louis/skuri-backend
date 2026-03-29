package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminCommentSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.AdminPostSummaryResponse;
import com.skuri.skuri_backend.domain.board.dto.response.BoardModerationResponse;
import com.skuri.skuri_backend.domain.board.dto.response.CommentLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.CommentResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostBookmarkResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostDetailResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostLikeResponse;
import com.skuri.skuri_backend.domain.board.dto.response.PostSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiBoardSchemas {

    private OpenApiBoardSchemas() {
    }

    @Schema(name = "BoardPostDetailApiResponse", description = "공통 API 응답 포맷")
    public record PostDetailApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PostDetailResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardPostSummaryPageApiResponse", description = "공통 API 응답 포맷")
    public record PostSummaryPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<PostSummaryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AdminBoardPostSummaryPageApiResponse", description = "공통 API 응답 포맷")
    public record AdminPostSummaryPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<AdminPostSummaryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AdminBoardPostDetailApiResponse", description = "공통 API 응답 포맷")
    public record AdminPostDetailApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminPostDetailResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AdminBoardCommentSummaryPageApiResponse", description = "공통 API 응답 포맷")
    public record AdminCommentSummaryPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<AdminCommentSummaryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardModerationApiResponse", description = "공통 API 응답 포맷")
    public record BoardModerationApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            BoardModerationResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardPostLikeApiResponse", description = "공통 API 응답 포맷")
    public record PostLikeApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PostLikeResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardPostBookmarkApiResponse", description = "공통 API 응답 포맷")
    public record PostBookmarkApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PostBookmarkResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardCommentListApiResponse", description = "공통 API 응답 포맷")
    public record CommentListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<CommentResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardCommentApiResponse", description = "공통 API 응답 포맷")
    public record CommentApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            CommentResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "BoardCommentLikeApiResponse", description = "공통 API 응답 포맷")
    public record CommentLikeApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            CommentLikeResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
