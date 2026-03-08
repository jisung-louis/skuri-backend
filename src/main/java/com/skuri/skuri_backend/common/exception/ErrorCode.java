package com.skuri.skuri_backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "관리자 권한이 필요합니다."),
    EMAIL_DOMAIN_RESTRICTED(HttpStatus.FORBIDDEN, "EMAIL_DOMAIN_RESTRICTED", "성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "리소스 충돌이 발생했습니다."),
    RESOURCE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "RESOURCE_CONCURRENT_MODIFICATION", "동시 수정 충돌이 발생했습니다. 다시 시도해주세요."),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "입력값 검증에 실패했습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다."),
    NOT_NOTIFICATION_OWNER(HttpStatus.FORBIDDEN, "NOT_NOTIFICATION_OWNER", "본인 알림만 접근할 수 있습니다."),
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND", "강의를 찾을 수 없습니다."),
    ACADEMIC_SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "ACADEMIC_SCHEDULE_NOT_FOUND", "학사 일정을 찾을 수 없습니다."),
    TIMETABLE_CONFLICT(HttpStatus.CONFLICT, "TIMETABLE_CONFLICT", "시간이 겹치는 강의가 이미 시간표에 있습니다."),
    COURSE_ALREADY_IN_TIMETABLE(HttpStatus.CONFLICT, "COURSE_ALREADY_IN_TIMETABLE", "이미 시간표에 추가된 강의입니다."),
    PARTY_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTY_NOT_FOUND", "파티를 찾을 수 없습니다."),
    PARTY_FULL(HttpStatus.CONFLICT, "PARTY_FULL", "파티 정원이 가득 찼습니다."),
    PARTY_CLOSED(HttpStatus.CONFLICT, "PARTY_CLOSED", "모집이 마감된 파티입니다."),
    PARTY_ENDED(HttpStatus.CONFLICT, "PARTY_ENDED", "이미 종료된 파티입니다."),
    NOT_PARTY_LEADER(HttpStatus.FORBIDDEN, "NOT_PARTY_LEADER", "리더 권한이 필요합니다."),
    NOT_PARTY_MEMBER(HttpStatus.FORBIDDEN, "NOT_PARTY_MEMBER", "파티 멤버가 아닙니다."),
    ALREADY_IN_PARTY(HttpStatus.CONFLICT, "ALREADY_IN_PARTY", "이미 파티에 참여 중입니다."),
    ALREADY_REQUESTED(HttpStatus.CONFLICT, "ALREADY_REQUESTED", "이미 동승 요청을 보냈습니다."),
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "REQUEST_NOT_FOUND", "동승 요청을 찾을 수 없습니다."),
    REQUEST_ALREADY_PROCESSED(HttpStatus.CONFLICT, "REQUEST_ALREADY_PROCESSED", "이미 처리된 요청입니다."),
    SETTLEMENT_NOT_COMPLETED(HttpStatus.CONFLICT, "SETTLEMENT_NOT_COMPLETED", "정산이 완료되지 않았습니다."),
    ALREADY_SETTLED(HttpStatus.CONFLICT, "ALREADY_SETTLED", "이미 정산 완료 처리된 멤버입니다."),
    PARTY_NOT_ARRIVABLE(HttpStatus.CONFLICT, "PARTY_NOT_ARRIVABLE", "도착 처리할 수 없는 파티 상태입니다."),
    PARTY_NOT_CANCELABLE(HttpStatus.CONFLICT, "PARTY_NOT_CANCELABLE", "취소할 수 없는 파티 상태입니다."),
    NO_MEMBERS_TO_SETTLE(HttpStatus.CONFLICT, "NO_MEMBERS_TO_SETTLE", "정산 대상 멤버가 없습니다."),
    LEADER_CANNOT_LEAVE(HttpStatus.CONFLICT, "LEADER_CANNOT_LEAVE", "리더는 파티에서 나갈 수 없습니다."),
    CANNOT_LEAVE_ARRIVED_PARTY(HttpStatus.CONFLICT, "CANNOT_LEAVE_ARRIVED_PARTY", "ARRIVED 상태에서는 파티를 나갈 수 없습니다."),
    CANNOT_KICK_IN_ARRIVED(HttpStatus.CONFLICT, "CANNOT_KICK_IN_ARRIVED", "ARRIVED 상태에서는 멤버를 강퇴할 수 없습니다."),
    CANNOT_KICK_LEADER(HttpStatus.BAD_REQUEST, "CANNOT_KICK_LEADER", "리더는 강퇴할 수 없습니다."),
    INVALID_PARTY_STATE_TRANSITION(HttpStatus.CONFLICT, "INVALID_PARTY_STATE_TRANSITION", "허용되지 않는 파티 상태 전이입니다."),
    PARTY_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "PARTY_CONCURRENT_MODIFICATION", "동시 요청 충돌이 발생했습니다. 다시 시도해주세요."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다."),
    NOT_CHAT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "NOT_CHAT_ROOM_MEMBER", "채팅방 멤버가 아닙니다."),
    CHAT_ROOM_FULL(HttpStatus.CONFLICT, "CHAT_ROOM_FULL", "채팅방 정원이 가득 찼습니다."),
    ALREADY_CHAT_ROOM_MEMBER(HttpStatus.CONFLICT, "ALREADY_CHAT_ROOM_MEMBER", "이미 채팅방에 참여 중입니다."),
    STOMP_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "STOMP_AUTH_FAILED", "WebSocket 인증에 실패했습니다."),
    BANK_ACCOUNT_REQUIRED(HttpStatus.UNPROCESSABLE_CONTENT, "BANK_ACCOUNT_REQUIRED", "계좌 정보 등록 후 이용 가능합니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),
    NOT_POST_AUTHOR(HttpStatus.FORBIDDEN, "NOT_POST_AUTHOR", "게시글 작성자만 수정/삭제할 수 있습니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "NOT_COMMENT_AUTHOR", "댓글 작성자만 수정/삭제할 수 있습니다."),
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다."),
    APP_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "APP_NOTICE_NOT_FOUND", "앱 공지를 찾을 수 없습니다."),
    NOTICE_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTICE_COMMENT_NOT_FOUND", "공지 댓글을 찾을 수 없습니다."),
    NOT_NOTICE_COMMENT_AUTHOR(HttpStatus.FORBIDDEN, "NOT_NOTICE_COMMENT_AUTHOR", "공지 댓글 작성자만 삭제할 수 있습니다."),
    COMMENT_ALREADY_DELETED(HttpStatus.CONFLICT, "COMMENT_ALREADY_DELETED", "이미 삭제된 댓글입니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."),
    INVALID_INQUIRY_STATUS_TRANSITION(HttpStatus.CONFLICT, "INVALID_INQUIRY_STATUS_TRANSITION", "허용되지 않는 문의 상태 전이입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "신고를 찾을 수 없습니다."),
    REPORT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "REPORT_ALREADY_SUBMITTED", "동일 대상에 대한 중복 신고입니다."),
    CANNOT_REPORT_YOURSELF(HttpStatus.BAD_REQUEST, "CANNOT_REPORT_YOURSELF", "자기 자신은 신고할 수 없습니다."),
    INVALID_REPORT_STATUS_TRANSITION(HttpStatus.CONFLICT, "INVALID_REPORT_STATUS_TRANSITION", "허용되지 않는 신고 상태 전이입니다."),
    CAFETERIA_MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "CAFETERIA_MENU_NOT_FOUND", "학식 메뉴를 찾을 수 없습니다."),
    CAFETERIA_MENU_ALREADY_EXISTS(HttpStatus.CONFLICT, "CAFETERIA_MENU_ALREADY_EXISTS", "이미 등록된 주차의 학식 메뉴입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
