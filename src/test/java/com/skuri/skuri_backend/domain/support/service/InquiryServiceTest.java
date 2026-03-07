package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    void updateInquiryStatus_pendingToResolved_성공() {
        Inquiry inquiry = Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 종료됩니다.",
                "user-1",
                "user@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234"
        );
        ReflectionTestUtils.setField(inquiry, "id", "inquiry-1");
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));

        AdminInquiryResponse response = inquiryService.updateInquiryStatus(
                "inquiry-1",
                new UpdateInquiryStatusRequest(InquiryStatus.RESOLVED, "재현 후 수정 배포 완료")
        );

        assertEquals(InquiryStatus.RESOLVED, response.status());
        assertEquals("재현 후 수정 배포 완료", response.memo());
    }

    @Test
    void updateInquiryStatus_resolvedToPending_실패() {
        Inquiry inquiry = Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 종료됩니다.",
                "user-1",
                "user@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234"
        );
        ReflectionTestUtils.setField(inquiry, "id", "inquiry-1");
        inquiry.updateStatus(InquiryStatus.RESOLVED, null);
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> inquiryService.updateInquiryStatus(
                        "inquiry-1",
                        new UpdateInquiryStatusRequest(InquiryStatus.PENDING, null)
                )
        );

        assertEquals(ErrorCode.INVALID_INQUIRY_STATUS_TRANSITION, exception.getErrorCode());
    }
}
