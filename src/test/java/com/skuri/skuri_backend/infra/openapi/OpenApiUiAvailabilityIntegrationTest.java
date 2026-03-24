package com.skuri.skuri_backend.infra.openapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiUiAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocs_json이_노출된다() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void swaggerUi_html이_노출된다() throws Exception {
        MvcResult result = mockMvc.perform(get("/swagger-ui/index.html"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertTrue(status >= 200 && status < 400, "swagger-ui 경로가 노출되어야 한다");

        if (status >= 200 && status < 300) {
            assertTrue(
                    result.getResponse().getContentType() != null
                            && result.getResponse().getContentType().contains(MediaType.TEXT_HTML_VALUE),
                    "swagger-ui는 HTML을 반환해야 한다"
            );
        } else {
            String location = result.getResponse().getHeader("Location");
            assertFalse(location == null || location.isBlank(), "redirect 응답이면 Location 헤더가 있어야 한다");
        }
    }

    @Test
    void scalar_html이_노출된다() throws Exception {
        MvcResult result = mockMvc.perform(get("/scalar"))
                .andReturn();

        int status = result.getResponse().getStatus();
        assertTrue(status >= 200 && status < 400, "scalar 경로가 노출되어야 한다");

        if (status >= 200 && status < 300) {
            assertTrue(
                    result.getResponse().getContentType() != null
                            && result.getResponse().getContentType().contains(MediaType.TEXT_HTML_VALUE),
                    "scalar는 HTML을 반환해야 한다"
            );
        } else {
            String location = result.getResponse().getHeader("Location");
            assertFalse(location == null || location.isBlank(), "redirect 응답이면 Location 헤더가 있어야 한다");
        }
    }
}
