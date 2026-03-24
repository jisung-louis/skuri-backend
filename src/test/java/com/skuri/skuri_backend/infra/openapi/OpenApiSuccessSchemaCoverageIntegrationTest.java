package com.skuri.skuri_backend.infra.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSuccessSchemaCoverageIntegrationTest {

    private static final Set<OperationKey> GENERIC_SUCCESS_ALLOWLIST = Set.of(
            new OperationKey("post", "/v1/members/me/fcm-tokens"),
            new OperationKey("delete", "/v1/members/me/fcm-tokens"),
            new OperationKey("delete", "/v1/notifications/{notificationId}"),
            new OperationKey("delete", "/v1/posts/{postId}"),
            new OperationKey("delete", "/v1/comments/{commentId}"),
            new OperationKey("delete", "/v1/notice-comments/{commentId}"),
            new OperationKey("delete", "/v1/admin/app-notices/{appNoticeId}"),
            new OperationKey("delete", "/v1/admin/cafeteria-menus/{weekId}"),
            new OperationKey("delete", "/v1/admin/chat-rooms/{chatRoomId}"),
            new OperationKey("delete", "/v1/admin/academic-schedules/{scheduleId}"),
            new OperationKey("delete", "/v1/parties/{id}/members/{memberId}"),
            new OperationKey("delete", "/v1/parties/{id}/members/me")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 대상_success_schema는_generic_data로_남지_않는다() throws Exception {
        JsonNode root = apiDocs();
        List<String> violations = new ArrayList<>();

        Iterator<String> pathNames = root.path("paths").fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathNode = root.path("paths").path(path);

            Iterator<String> methods = pathNode.fieldNames();
            while (methods.hasNext()) {
                String method = methods.next().toLowerCase(Locale.ROOT);
                JsonNode operation = pathNode.path(method);

                Iterator<String> responseCodes = operation.path("responses").fieldNames();
                while (responseCodes.hasNext()) {
                    String responseCode = responseCodes.next();
                    if (!responseCode.startsWith("2")) {
                        continue;
                    }

                    JsonNode successContent = operation.path("responses")
                            .path(responseCode)
                            .path("content")
                            .path("application/json");
                    if (successContent.isMissingNode()) {
                        continue;
                    }

                    OperationKey key = new OperationKey(method, path);
                    if (GENERIC_SUCCESS_ALLOWLIST.contains(key)) {
                        continue;
                    }

                    JsonNode successSchema = resolveSchema(root, successContent.path("schema"));
                    JsonNode dataSchema = resolveSchema(root, successSchema.path("properties").path("data"));

                    if (!hasConcreteShape(dataSchema)) {
                        violations.add(method.toUpperCase(Locale.ROOT) + " " + path + " [" + responseCode + "]");
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Show schema concrete data 누락 API:\n" + String.join("\n", violations)
        );
    }

    private JsonNode apiDocs() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode resolveSchema(JsonNode root, JsonNode schemaNode) {
        JsonNode current = schemaNode;
        while (current != null && current.has("$ref")) {
            String ref = current.path("$ref").asText();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            current = root.path("components").path("schemas").path(schemaName);
        }
        return current;
    }

    private boolean hasConcreteShape(JsonNode schemaNode) {
        if (schemaNode == null || schemaNode.isMissingNode() || schemaNode.isNull()) {
            return false;
        }

        if (schemaNode.has("properties") && schemaNode.path("properties").size() > 0) {
            return true;
        }
        if (schemaNode.has("items")) {
            return true;
        }
        if (schemaNode.has("oneOf") || schemaNode.has("anyOf") || schemaNode.has("allOf")) {
            return true;
        }
        return false;
    }

    private record OperationKey(String method, String path) {
    }
}
