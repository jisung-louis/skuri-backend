package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiBoardExamples {

    private OpenApiBoardExamples() {
    }

    public static final String SUCCESS_POST_CREATE = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "title": "게시글 제목",
                "content": "게시글 전체 내용",
                "authorId": "user_uuid",
                "authorName": "홍길동",
                "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                "isAnonymous": false,
                "category": "GENERAL",
                "viewCount": 0,
                "likeCount": 0,
                "commentCount": 0,
                "bookmarkCount": 0,
                "images": [
                  {
                    "url": "https://cdn.skuri.app/posts/post-1/image-1.jpg",
                    "thumbUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                    "width": 800,
                    "height": 600,
                    "size": 245123,
                    "mime": "image/jpeg"
                  }
                ],
                "isLiked": false,
                "isBookmarked": false,
                "isAuthor": true,
                "createdAt": "2026-03-05T20:30:00",
                "updatedAt": "2026-03-05T20:30:00"
              }
            }
            """;

    public static final String SUCCESS_POST_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "post_uuid",
                    "title": "게시글 제목",
                    "content": "내용 미리보기...",
                    "authorId": "user_uuid",
                    "authorName": "홍길동",
                    "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                    "isAnonymous": false,
                    "category": "GENERAL",
                    "viewCount": 100,
                    "likeCount": 10,
                    "commentCount": 5,
                    "bookmarkCount": 3,
                    "isLiked": true,
                    "isBookmarked": false,
                    "isCommentedByMe": true,
                    "hasImage": true,
                    "thumbnailUrl": "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg",
                    "isPinned": false,
                    "createdAt": "2026-02-03T12:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 100,
                "totalPages": 5,
                "hasNext": true,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_POST_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "post_uuid",
                "title": "게시글 제목",
                "content": "게시글 전체 내용",
                "authorId": "user_uuid",
                "authorName": "홍길동",
                "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                "isAnonymous": false,
                "category": "GENERAL",
                "viewCount": 101,
                "likeCount": 10,
                "commentCount": 5,
                "bookmarkCount": 3,
                "images": [],
                "isLiked": true,
                "isBookmarked": false,
                "isAuthor": true,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_LIKE = """
            {
              "success": true,
              "data": {
                "isLiked": true,
                "likeCount": 11
              }
            }
            """;

    public static final String SUCCESS_BOOKMARK = """
            {
              "success": true,
              "data": {
                "isBookmarked": true,
                "bookmarkCount": 4
              }
            }
            """;

    public static final String SUCCESS_COMMENTS_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "comment_uuid",
                  "parentId": null,
                  "depth": 0,
                  "content": "댓글 내용",
                  "authorId": "user_uuid",
                  "authorName": "홍길동",
                  "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                  "isAnonymous": false,
                  "anonymousOrder": null,
                  "isAuthor": false,
                  "isPostAuthor": true,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:00:00",
                  "updatedAt": "2026-02-03T12:00:00"
                },
                {
                  "id": "reply_uuid",
                  "parentId": "comment_uuid",
                  "depth": 1,
                  "content": "대댓글 내용",
                  "authorId": null,
                  "authorName": "익명2",
                  "authorProfileImage": null,
                  "isAnonymous": true,
                  "anonymousOrder": 2,
                  "isAuthor": false,
                  "isPostAuthor": false,
                  "isDeleted": false,
                  "createdAt": "2026-02-03T12:30:00",
                  "updatedAt": "2026-02-03T12:30:00"
                }
              ]
            }
            """;

    public static final String SUCCESS_COMMENT_CREATE = """
            {
              "success": true,
              "data": {
                "id": "comment_uuid",
                "parentId": "root_comment_uuid",
                "depth": 2,
                "content": "댓글 내용",
                "authorId": null,
                "authorName": "익명1",
                "authorProfileImage": null,
                "isAnonymous": true,
                "anonymousOrder": 1,
                "isAuthor": true,
                "isPostAuthor": false,
                "isDeleted": false,
                "createdAt": "2026-02-03T12:00:00",
                "updatedAt": "2026-02-03T12:00:00"
              }
            }
            """;

    public static final String SUCCESS_COMMENT_DELETED = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String SUCCESS_MEMBER_POSTS_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "post_uuid",
                    "title": "내가 작성한 글",
                    "content": "내용 미리보기...",
                    "authorId": "user_uuid",
                    "authorName": "홍길동",
                    "authorProfileImage": "https://cdn.skuri.app/profiles/user-1.png",
                    "isAnonymous": false,
                    "category": "GENERAL",
                    "viewCount": 100,
                    "likeCount": 10,
                    "commentCount": 5,
                    "bookmarkCount": 4,
                    "isLiked": true,
                    "isBookmarked": true,
                    "isCommentedByMe": false,
                    "hasImage": false,
                    "thumbnailUrl": null,
                    "isPinned": false,
                    "createdAt": "2026-02-03T12:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 12,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String ERROR_POST_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "POST_NOT_FOUND",
              "message": "게시글을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_COMMENT_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "COMMENT_NOT_FOUND",
              "message": "댓글을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_NOT_POST_AUTHOR = """
            {
              "success": false,
              "errorCode": "NOT_POST_AUTHOR",
              "message": "게시글 작성자만 수정/삭제할 수 있습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_NOT_COMMENT_AUTHOR = """
            {
              "success": false,
              "errorCode": "NOT_COMMENT_AUTHOR",
              "message": "댓글 작성자만 수정/삭제할 수 있습니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;

    public static final String ERROR_COMMENT_ALREADY_DELETED = """
            {
              "success": false,
              "errorCode": "COMMENT_ALREADY_DELETED",
              "message": "이미 삭제된 댓글입니다.",
              "timestamp": "2026-03-05T20:30:00"
            }
            """;
}
