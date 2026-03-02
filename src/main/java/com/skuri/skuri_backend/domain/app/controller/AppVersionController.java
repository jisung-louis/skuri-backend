package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/app-versions")
public class AppVersionController {

    @GetMapping("/{platform}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAppVersion(@PathVariable String platform) {
        Map<String, Object> response = Map.of(
                "platform", platform,
                "minVersion", "1.0.0",
                "latestVersion", "1.0.0",
                "forceUpdate", false
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
