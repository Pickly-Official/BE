package com.be.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "존재하지 않는 리소스입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C003", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C004", "접근 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 내부 오류입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
    USER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "U002", "이미 탈퇴한 사용자입니다."),

    // Vote
    VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "존재하지 않는 투표입니다."),
    VOTE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "V002", "이미 종료된 투표입니다."),

    // Photo
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 사진입니다."),
    INVALID_PHOTO_COUNT(HttpStatus.BAD_REQUEST, "P002", "사진은 2장 이상 10장 이하로 업로드해야 합니다."),
    NO_EXIF_GPS(HttpStatus.BAD_REQUEST, "P003", "위치정보가 없는 사진입니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "P004", "이미지 파일만 업로드할 수 있습니다."),

    // Participant
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "PA001", "존재하지 않는 참여자입니다."),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "PA002", "이미 참여한 투표입니다."),

    // JWT
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "J002", "만료된 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
