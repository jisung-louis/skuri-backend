package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.academic.controller.AcademicScheduleAdminController;
import com.skuri.skuri_backend.domain.academic.controller.CourseAdminController;
import com.skuri.skuri_backend.domain.app.controller.AppNoticeAdminController;
import com.skuri.skuri_backend.domain.chat.controller.ChatAdminRoomController;
import com.skuri.skuri_backend.domain.member.controller.MemberAdminController;
import com.skuri.skuri_backend.domain.notice.controller.NoticeAdminController;
import com.skuri.skuri_backend.domain.support.controller.AppVersionAdminController;
import com.skuri.skuri_backend.domain.support.controller.CafeteriaMenuAdminController;
import com.skuri.skuri_backend.domain.support.controller.InquiryAdminController;
import com.skuri.skuri_backend.domain.support.controller.ReportAdminController;
import com.skuri.skuri_backend.domain.taxiparty.controller.PartyAdminController;
import com.skuri.skuri_backend.infra.auth.config.AdminApiAccess;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminOpenApiConventionTest {

    private static final List<Class<?>> ADMIN_CONTROLLERS = List.of(
            AcademicScheduleAdminController.class,
            CourseAdminController.class,
            ChatAdminRoomController.class,
            MemberAdminController.class,
            NoticeAdminController.class,
            AppNoticeAdminController.class,
            InquiryAdminController.class,
            ReportAdminController.class,
            AppVersionAdminController.class,
            CafeteriaMenuAdminController.class,
            PartyAdminController.class
    );

    @Test
    void adminController는_공통_AdminApiAccess를_사용한다() {
        assertThat(ADMIN_CONTROLLERS)
                .allMatch(controller -> controller.isAnnotationPresent(AdminApiAccess.class));
    }

    @Test
    void adminController의_403응답예시는_ADMIN_REQUIRED를_재사용한다() {
        for (Class<?> controller : ADMIN_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                for (ApiResponse apiResponse : resolveResponses(method)) {
                    if (!"403".equals(apiResponse.responseCode())) {
                        continue;
                    }

                    Content[] contents = apiResponse.content();
                    assertThat(contents)
                            .withFailMessage("%s#%s 는 403 content 가 비어 있습니다.", controller.getSimpleName(), method.getName())
                            .isNotEmpty();

                    for (Content content : contents) {
                        assertThat(content.examples())
                                .withFailMessage("%s#%s 는 403 examples 가 비어 있습니다.", controller.getSimpleName(), method.getName())
                                .isNotEmpty();
                        assertThat(Arrays.stream(content.examples()).map(example -> example.value()).toList())
                                .withFailMessage("%s#%s 는 403 예시에서 ERROR_ADMIN_REQUIRED 를 재사용해야 합니다.", controller.getSimpleName(), method.getName())
                                .containsOnly(OpenApiCommonExamples.ERROR_ADMIN_REQUIRED);
                    }
                }
            }
        }
    }

    private static List<ApiResponse> resolveResponses(Method method) {
        ApiResponses apiResponses = method.getAnnotation(ApiResponses.class);
        if (apiResponses == null) {
            return List.of();
        }
        return Arrays.asList(apiResponses.value());
    }
}
