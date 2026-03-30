package com.skuri.skuri_backend.domain.minecraft.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MinecraftInternalSecretFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Skuri-Minecraft-Secret";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final MinecraftBridgeProperties bridgeProperties;

    public MinecraftInternalSecretFilter(
            MinecraftBridgeProperties bridgeProperties
    ) {
        this.bridgeProperties = bridgeProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/minecraft/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String expectedSecret = bridgeProperties.sharedSecret();
        String providedSecret = request.getHeader(HEADER_NAME);

        if (StringUtils.hasText(expectedSecret) && expectedSecret.equals(providedSecret)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(ErrorCode.MINECRAFT_SECRET_INVALID.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(
                response.getWriter(),
                ApiResponse.error(
                        ErrorCode.MINECRAFT_SECRET_INVALID.getCode(),
                        ErrorCode.MINECRAFT_SECRET_INVALID.getMessage()
                )
        );
    }
}
