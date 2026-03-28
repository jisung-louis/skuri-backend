package com.skuri.skuri_backend.domain.support.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryAttachmentRequest;
import com.skuri.skuri_backend.domain.support.dto.request.CreateInquiryRequest;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.request.UpdateInquiryStatusRequest;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.entity.Inquiry;
import com.skuri.skuri_backend.domain.support.entity.InquiryAttachment;
import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import com.skuri.skuri_backend.domain.support.repository.InquiryRepository;
import com.skuri.skuri_backend.infra.auth.firebase.AuthenticatedMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    private static final AuthenticatedMember AUTHENTICATED_MEMBER = new AuthenticatedMember(
            "user-1",
            "user@sungkyul.ac.kr",
            "google.com",
            "provider-id",
            "홍길동",
            "https://example.com/profile.jpg"
    );

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
                List.of(),
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
                List.of(),
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

    @Test
    void getAdminInquiries_음수페이지_실패() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> inquiryService.getAdminInquiries(null, -1, 20)
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        assertEquals("page는 0 이상이어야 합니다.", exception.getMessage());
    }

    @Test
    void anonymizeWithdrawnMemberInquiries_구조화개인정보를마스킹한다() {
        Inquiry inquiry = Inquiry.create(
                InquiryType.BUG,
                "채팅 오류",
                "채팅 진입 시 종료됩니다.",
                List.of(new InquiryAttachment(
                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                        800,
                        600,
                        245123,
                        "image/jpeg"
                )),
                "user-1",
                "user@sungkyul.ac.kr",
                "스쿠리유저",
                "홍길동",
                "20201234"
        );

        when(inquiryRepository.findAllByUserId("user-1")).thenReturn(List.of(inquiry));

        inquiryService.anonymizeWithdrawnMemberInquiries("user-1");

        assertEquals("탈퇴한 사용자", inquiry.getUserName());
        assertNull(inquiry.getUserEmail());
        assertNull(inquiry.getUserRealname());
        assertNull(inquiry.getUserStudentId());
        assertEquals(1, inquiry.getAttachments().size());
    }

    @Test
    void createInquiry_attachmentsNull이면_빈배열로저장한다() {
        when(memberRepository.findById("user-1")).thenReturn(Optional.empty());
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inquiryService.createInquiry(
                AUTHENTICATED_MEMBER,
                new CreateInquiryRequest(InquiryType.BUG, "앱 오류 문의", "채팅 화면에서 오류가 발생합니다.", null)
        );

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        assertIterableEquals(List.of(), captor.getValue().getAttachments());
    }

    @Test
    void createInquiry_첨부메타데이터를정규화해저장한다() {
        when(memberRepository.findById("user-1")).thenReturn(Optional.empty());
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InquiryAttachment expectedAttachment = new InquiryAttachment(
                "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                800,
                600,
                245123,
                "image/jpeg"
        );
        inquiryService.createInquiry(
                AUTHENTICATED_MEMBER,
                new CreateInquiryRequest(
                        InquiryType.BUG,
                        "앱 오류 문의",
                        "채팅 화면에서 오류가 발생합니다.",
                        List.of(new CreateInquiryAttachmentRequest(
                                expectedAttachment.url(),
                                expectedAttachment.thumbUrl(),
                                expectedAttachment.width(),
                                expectedAttachment.height(),
                                expectedAttachment.size(),
                                expectedAttachment.mime()
                        ))
                )
        );

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        assertIterableEquals(List.of(expectedAttachment), captor.getValue().getAttachments());
    }

    @Test
    void createInquiry_허용되지않는첨부mime이면_검증예외() {
        when(memberRepository.findById("user-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> inquiryService.createInquiry(
                        AUTHENTICATED_MEMBER,
                        new CreateInquiryRequest(
                                InquiryType.BUG,
                                "앱 오류 문의",
                                "채팅 화면에서 오류가 발생합니다.",
                                List.of(new CreateInquiryAttachmentRequest(
                                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image.jpg",
                                        "https://cdn.skuri.app/uploads/inquiries/2026/03/28/image_thumb.jpg",
                                        800,
                                        600,
                                        245123,
                                        "application/pdf"
                                ))
                        )
                )
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }
}
