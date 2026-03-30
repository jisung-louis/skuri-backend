package com.skuri.skuri_backend.domain.minecraft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class MojangHttpProfileLookupClient implements MojangProfileLookupClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper;
    private final MinecraftIdentityService minecraftIdentityService;

    public MojangHttpProfileLookupClient(
            ObjectMapper objectMapper,
            MinecraftIdentityService minecraftIdentityService
    ) {
        this.objectMapper = objectMapper;
        this.minecraftIdentityService = minecraftIdentityService;
    }

    @Override
    public MojangProfile lookup(String gameName) {
        String trimmed = gameName == null ? "" : gameName.trim();
        if (!StringUtils.hasText(trimmed)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "gameName은 필수입니다.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.mojang.com/users/profiles/minecraft/"
                                + URLEncoder.encode(trimmed, StandardCharsets.UTF_8)
                ))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204 || response.statusCode() == 404) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 닉네임을 찾을 수 없습니다.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Mojang 서버 조회 중 오류가 발생했습니다.");
            }

            JsonNode body = objectMapper.readTree(response.body());
            String normalizedUuid = minecraftIdentityService.normalizeJavaUuid(body.path("id").asText());
            String resolvedName = body.path("name").asText(trimmed);
            return new MojangProfile(resolvedName, normalizedUuid);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Mojang 응답을 파싱하지 못했습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Mojang 서버 조회가 중단되었습니다.");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Mojang 서버 조회 중 오류가 발생했습니다.");
        }
    }
}
