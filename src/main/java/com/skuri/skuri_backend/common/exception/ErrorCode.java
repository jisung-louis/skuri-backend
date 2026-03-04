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
    EMAIL_DOMAIN_RESTRICTED(HttpStatus.FORBIDDEN, "EMAIL_DOMAIN_RESTRICTED", "성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "리소스 충돌이 발생했습니다."),
    RESOURCE_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "RESOURCE_CONCURRENT_MODIFICATION", "동시 수정 충돌이 발생했습니다. 다시 시도해주세요."),
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "입력값 검증에 실패했습니다."),
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
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
