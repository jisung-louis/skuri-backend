package com.skuri.skuri_backend.domain.member.controller;

import com.skuri.skuri_backend.common.dto.ApiResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberBankAccountRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberNotificationSettingsRequest;
import com.skuri.skuri_backend.domain.member.dto.request.UpdateMemberProfileRequest;
import com.skuri.skuri_backend.domain.member.dto.response.MemberCreateResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberPublicProfileResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberUpsertResult;
import com.skuri.skuri_backend.domain.member.service.MemberService;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberCreateResponse>> createMember(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        MemberUpsertResult result = memberService.createMember(requireAuthenticatedMember(authenticatedMember));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(ApiResponse.success(result.member()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> getMyProfile(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember
    ) {
        MemberMeResponse response = memberService.getMyProfile(requireAuthenticatedMember(authenticatedMember).uid());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyProfile(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberProfileRequest request
    ) {
        MemberMeResponse response = memberService.updateMyProfile(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/bank-account")
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyBankAccount(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberBankAccountRequest request
    ) {
        MemberMeResponse response = memberService.updateMyBankAccount(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me/notification-settings")
    public ResponseEntity<ApiResponse<MemberMeResponse>> updateMyNotificationSettings(
            @AuthenticationPrincipal AuthenticatedMember authenticatedMember,
            @Valid @RequestBody UpdateMemberNotificationSettingsRequest request
    ) {
        MemberMeResponse response = memberService.updateMyNotificationSettings(requireAuthenticatedMember(authenticatedMember).uid(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MemberPublicProfileResponse>> getMember(
            @PathVariable("id") String memberId
    ) {
        MemberPublicProfileResponse response = memberService.getMemberById(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private AuthenticatedMember requireAuthenticatedMember(AuthenticatedMember authenticatedMember) {
        if (authenticatedMember == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authenticatedMember;
    }
}
