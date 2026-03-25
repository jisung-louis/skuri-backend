package com.skuri.skuri_backend.infra.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "firebase-id-token";

    @Bean
    public OpenAPI skuriOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SKURI Backend API")
                        .description("Firebase ID Token 기반 SKURI 백엔드 API 명세")
                        .version("v1")
                        .contact(new Contact().name("SKURI Backend Team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Firebase Authentication ID Token")
                ));
    }

    @Bean
    public GroupedOpenApi memberApi() {
        return GroupedOpenApi.builder()
                .group("member")
                .pathsToMatch("/v1/members/**")
                .pathsToExclude(
                        "/v1/members/me/posts",
                        "/v1/members/me/bookmarks",
                        "/v1/members/me/fcm-tokens"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi supportApi() {
        return GroupedOpenApi.builder()
                .group("support")
                .pathsToMatch(
                        "/v1/app-versions/**",
                        "/v1/inquiries/**",
                        "/v1/reports/**",
                        "/v1/cafeteria-menus/**",
                        "/v1/admin/inquiries/**",
                        "/v1/admin/reports/**",
                        "/v1/admin/app-versions/**",
                        "/v1/admin/cafeteria-menus/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi noticeApi() {
        return GroupedOpenApi.builder()
                .group("notice")
                .pathsToMatch(
                        "/v1/notices/**",
                        "/v1/notice-comments/**",
                        "/v1/app-notices/**",
                        "/v1/admin/notices/**",
                        "/v1/admin/app-notices/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi campusApi() {
        return GroupedOpenApi.builder()
                .group("campus")
                .pathsToMatch(
                        "/v1/campus-banners/**",
                        "/v1/admin/campus-banners/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi taxiPartyApi() {
        return GroupedOpenApi.builder()
                .group("taxiparty")
                .pathsToMatch(
                        "/v1/parties/**",
                        "/v1/join-requests/**",
                        "/v1/members/me/parties",
                        "/v1/members/me/join-requests",
                        "/v1/sse/parties/**",
                        "/v1/sse/members/me/join-requests"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("chat")
                .pathsToMatch("/v1/chat-rooms/**", "/v1/admin/chat-rooms/**")
                .build();
    }

    @Bean
    public GroupedOpenApi boardApi() {
        return GroupedOpenApi.builder()
                .group("board")
                .pathsToMatch(
                        "/v1/posts/**",
                        "/v1/comments/**",
                        "/v1/members/me/posts",
                        "/v1/members/me/bookmarks"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi imageApi() {
        return GroupedOpenApi.builder()
                .group("image")
                .pathsToMatch("/v1/images/**")
                .build();
    }

    @Bean
    public GroupedOpenApi academicApi() {
        return GroupedOpenApi.builder()
                .group("academic")
                .pathsToMatch(
                        "/v1/courses",
                        "/v1/timetables/**",
                        "/v1/academic-schedules/**",
                        "/v1/admin/academic-schedules/**",
                        "/v1/admin/courses",
                        "/v1/admin/courses/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi notificationApi() {
        return GroupedOpenApi.builder()
                .group("notification")
                .pathsToMatch(
                        "/v1/notifications/**",
                        "/v1/members/me/fcm-tokens",
                        "/v1/sse/notifications"
                )
                .build();
    }
}
