package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/app-notices")
public class AppNoticeController {

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAppNotices() {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}
