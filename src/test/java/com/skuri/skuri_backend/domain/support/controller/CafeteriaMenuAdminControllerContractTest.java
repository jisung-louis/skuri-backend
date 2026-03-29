package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuBadgeResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuCategoryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuEntryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.service.CafeteriaMenuService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CafeteriaMenuAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class CafeteriaMenuAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CafeteriaMenuService cafeteriaMenuService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void createMenu_관리자정상요청_201() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.createMenu(any())).thenReturn(menuResponse());

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(createRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weekId").value("2026-W08"))
                .andExpect(jsonPath("$.data.menuEntries['2026-02-16'].rollNoodles[0].likeCount").value(0))
                .andExpect(jsonPath("$.data.menuEntries['2026-02-16'].rollNoodles[0].myReaction").isEmpty());
    }

    @Test
    void createMenu_기존menus요청도_201() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.createMenu(any())).thenReturn(menuResponse());

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(legacyCreateRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weekId").value("2026-W08"));
    }

    @Test
    void createMenu_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content(createRequest())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void createMenu_검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "weekId": "",
                                          "weekStart": null,
                                          "weekEnd": null,
                                          "menus": {}
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createMenu_이미등록된주차_409() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.createMenu(any()))
                .thenThrow(new BusinessException(ErrorCode.CAFETERIA_MENU_ALREADY_EXISTS));

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(createRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_ALREADY_EXISTS"));
    }

    @Test
    void createMenu_주간동일메뉴메타데이터충돌_400() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.createMenu(any()))
                .thenThrow(new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "같은 주차에서 동일 카테고리의 동일 메뉴는 날짜별 메타데이터가 동일해야 합니다. "
                                + "category=rollNoodles, title=존슨부대찌개, firstDate=2026-02-16, date=2026-02-17"
                ));

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(weeklyConflictCreateRequest())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(
                        "같은 주차에서 동일 카테고리의 동일 메뉴는 날짜별 메타데이터가 동일해야 합니다. "
                                + "category=rollNoodles, title=존슨부대찌개, firstDate=2026-02-16, date=2026-02-17"
                ));
    }

    @Test
    void createMenu_카테고리코드형식오류_400() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.createMenu(any()))
                .thenThrow(new BusinessException(
                        ErrorCode.VALIDATION_ERROR,
                        "menuEntries.category는 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."
                ));

        mockMvc.perform(
                        post("/v1/admin/cafeteria-menus")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(invalidCategoryCreateRequest())
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("menuEntries.category는 영문, 숫자, 밑줄(_), 하이픈(-)만 사용할 수 있습니다."));
    }

    @Test
    void updateMenu_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.updateMenu(eq("2026-W08"), any())).thenReturn(menuResponse());

        mockMvc.perform(
                        put("/v1/admin/cafeteria-menus/2026-W08")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(updateRequest())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.weekId").value("2026-W08"));
    }

    @Test
    void updateMenu_없는주차_404() throws Exception {
        mockToken("admin-token", true);
        when(cafeteriaMenuService.updateMenu(eq("2026-W08"), any()))
                .thenThrow(new BusinessException(ErrorCode.CAFETERIA_MENU_NOT_FOUND));

        mockMvc.perform(
                        put("/v1/admin/cafeteria-menus/2026-W08")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(updateRequest())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_NOT_FOUND"));
    }

    @Test
    void deleteMenu_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(delete("/v1/admin/cafeteria-menus/2026-W08").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteMenu_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(delete("/v1/admin/cafeteria-menus/2026-W08").header(AUTHORIZATION, "Bearer user-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void deleteMenu_없는주차_404() throws Exception {
        mockToken("admin-token", true);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.CAFETERIA_MENU_NOT_FOUND))
                .when(cafeteriaMenuService).deleteMenu("2026-W08");

        mockMvc.perform(delete("/v1/admin/cafeteria-menus/2026-W08").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CAFETERIA_MENU_NOT_FOUND"));
    }

    private CafeteriaMenuResponse menuResponse() {
        return new CafeteriaMenuResponse(
                "2026-W08",
                LocalDate.of(2026, 2, 16),
                LocalDate.of(2026, 2, 20),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles", List.of("우동", "김밥"),
                                "theBab", List.of("돈까스", "된장찌개")
                        )
                ),
                List.of(
                        new CafeteriaMenuCategoryResponse("rollNoodles", "Roll & Noodles"),
                        new CafeteriaMenuCategoryResponse("theBab", "The bab"),
                        new CafeteriaMenuCategoryResponse("fryRice", "Fry & Rice")
                ),
                Map.of(
                        "2026-02-16",
                        Map.of(
                                "rollNoodles", List.of(
                                        new CafeteriaMenuEntryResponse(
                                                "2026-W08.rollNoodles.c4973864db4f8815",
                                                "존슨부대찌개",
                                                List.of(new CafeteriaMenuBadgeResponse("TAKEOUT", "테이크아웃")),
                                                0,
                                                0,
                                                null
                                        )
                                ),
                                "theBab", List.of(),
                                "fryRice", List.of()
                        )
                )
        );
    }

    private String createRequest() {
        return """
                {
                  "weekId": "2026-W08",
                  "weekStart": "2026-02-16",
                  "weekEnd": "2026-02-20",
                  "menuEntries": {
                    "2026-02-16": {
                      "rollNoodles": [
                        {
                          "title": "존슨부대찌개",
                          "badges": [
                            {
                              "label": "테이크아웃"
                            }
                          ],
                          "likeCount": 178,
                          "dislikeCount": 22
                        }
                      ],
                      "theBab": [],
                      "fryRice": []
                    }
                  }
                }
                """;
    }

    private String legacyCreateRequest() {
        return """
                {
                  "weekId": "2026-W08",
                  "weekStart": "2026-02-16",
                  "weekEnd": "2026-02-20",
                  "menus": {
                    "2026-02-16": {
                      "rollNoodles": ["우동", "김밥"],
                      "theBab": ["돈까스", "된장찌개"]
                    }
                  }
                }
                """;
    }

    private String updateRequest() {
        return """
                {
                  "weekStart": "2026-02-16",
                  "weekEnd": "2026-02-20",
                  "menuEntries": {
                    "2026-02-16": {
                      "rollNoodles": [
                        {
                          "title": "존슨부대찌개",
                          "badges": [
                            {
                              "code": "TAKEOUT",
                              "label": "테이크아웃"
                            }
                          ],
                          "likeCount": 178,
                          "dislikeCount": 22
                        }
                      ],
                      "theBab": [],
                      "fryRice": []
                    }
                  }
                }
                """;
    }

    private String weeklyConflictCreateRequest() {
        return """
                {
                  "weekId": "2026-W08",
                  "weekStart": "2026-02-16",
                  "weekEnd": "2026-02-20",
                  "menuEntries": {
                    "2026-02-16": {
                      "rollNoodles": [
                        {
                          "title": "존슨부대찌개",
                          "badges": [
                            {
                              "code": "TAKEOUT",
                              "label": "테이크아웃"
                            }
                          ],
                          "likeCount": 178,
                          "dislikeCount": 22
                        }
                      ]
                    },
                    "2026-02-17": {
                      "rollNoodles": [
                        {
                          "title": "존슨부대찌개",
                          "badges": [
                            {
                              "code": "TAKEOUT",
                              "label": "테이크아웃"
                            }
                          ],
                          "likeCount": 179,
                          "dislikeCount": 22
                        }
                      ]
                    }
                  }
                }
                """;
    }

    private String invalidCategoryCreateRequest() {
        return """
                {
                  "weekId": "2026-W08",
                  "weekStart": "2026-02-16",
                  "weekEnd": "2026-02-20",
                  "menuEntries": {
                    "2026-02-16": {
                      "special.v1": [
                        {
                          "title": "우동",
                          "badges": []
                        }
                      ]
                    }
                  }
                }
                """;
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반유저",
                        "https://example.com/profile.jpg"
                ));
        if (!admin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member member = Member.create(uid, uid + "@sungkyul.ac.kr", "관리자", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(member));
    }
}
