package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppVersionController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AppVersionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getAppVersion_비인증정상요청_200() throws Exception {
        when(appVersionService.getAppVersion("ios"))
                .thenReturn(new AppVersionResponse(
                        "ios",
                        "1.5.0",
                        false,
                        "새로운 기능이 추가되었습니다.",
                        "업데이트 안내",
                        true,
                        "업데이트",
                        "https://apps.apple.com/..."
                ));

        mockMvc.perform(get("/v1/app-versions/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("ios"))
                .andExpect(jsonPath("$.data.minimumVersion").value("1.5.0"));
    }

    @Test
    void getAppVersion_잘못된플랫폼_400() throws Exception {
        when(appVersionService.getAppVersion("windows"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 platform입니다."));

        mockMvc.perform(get("/v1/app-versions/windows"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void getAppVersion_버전정보없을때기본값응답_200() throws Exception {
        when(appVersionService.getAppVersion("ios"))
                .thenReturn(new AppVersionResponse(
                        "ios",
                        "1.0.0",
                        false,
                        null,
                        null,
                        false,
                        null,
                        null
                ));

        mockMvc.perform(get("/v1/app-versions/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minimumVersion").value("1.0.0"))
                .andExpect(jsonPath("$.data.forceUpdate").value(false))
                .andExpect(jsonPath("$.data.showButton").value(false));
    }
}
