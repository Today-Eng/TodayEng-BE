package com.example.todayEng.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드를 한 곳에서 관리한다.
 * 새 도메인이 생기면 접두어를 붙여 이 아래에 추가한다. (예: U001 = User, O001 = Order)
 */
@Getter
public enum ErrorCode {


    // Common (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "요청 값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "존재하지 않는 엔드포인트(URL)입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C006", "요청 파라미터의 타입이 올바르지 않습니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "C007", "필수 요청 파라미터가 누락되었습니다."),
    INVALID_HTTP_BODY(HttpStatus.BAD_REQUEST, "C008", "HTTP 요청 바디(JSON) 파싱에 실패했습니다."),
    MULTIPART_FILE_ERROR(HttpStatus.BAD_REQUEST, "C009", "파일 업로드 처리 중 오류가 발생했습니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "C010", "이미 존재하는 리소스입니다."),


    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A005", "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A006", "헤더에 토큰이 존재하지 않습니다."),


    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U003", "이미 사용중인 닉네임입니다."),
    EXTERNAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "U004", "연동된 외부 계정을 찾을 수 없습니다."),


    // External Service / S3 / API (E)
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "S3 파일 업로드에 실패했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "E002", "파일 업로드 용량을 초과했습니다."),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "E003", "허용되지 않는 파일 확장자입니다."),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E004", "외부 연동 API 호출에 실패했습니다."),
    GOOGLE_OAUTH_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "E005", "Google Calendar OAuth 설정이 완료되지 않았습니다."),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "E006", "유효하지 않은 OAuth 요청입니다."),
    OAUTH_STATE_EXPIRED(HttpStatus.BAD_REQUEST, "E007", "OAuth 인증 요청이 만료되었습니다."),
    OAUTH_STATE_ALREADY_USED(HttpStatus.BAD_REQUEST, "E008", "이미 처리된 OAuth 인증 요청입니다."),
    OAUTH_AUTHORIZATION_DENIED(HttpStatus.BAD_REQUEST, "E009", "외부 서비스 접근 권한이 거부되었습니다."),
    GOOGLE_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "E010", "Google OAuth 토큰 발급에 실패했습니다."),
    GOOGLE_USER_INFO_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "E011", "Google 계정 정보를 조회하지 못했습니다."),
    EXTERNAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "E012", "이미 다른 사용자에게 연동된 외부 계정입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "E013", "지원하지 않는 외부 서비스 provider입니다."),

    // Diary (D)
    MAIN_QUESTION_CANNOT_HAVE_PARENT(HttpStatus.BAD_REQUEST, "D001", "메인 질문에는 상위 질문을 지정할 수 없습니다."),
    FOLLOW_UP_QUESTION_REQUIRES_PARENT(HttpStatus.BAD_REQUEST, "D002", "후속 질문에는 메인 질문이 필요합니다."),
    FOLLOW_UP_PARENT_MUST_BE_MAIN_QUESTION(HttpStatus.BAD_REQUEST,"D003","후속 질문의 상위 질문은 메인 질문이어야 합니다."),
    FOLLOW_UP_QUESTION_CANNOT_HAVE_KEYWORD(HttpStatus.BAD_REQUEST, "D004", "후속 질문에는 키워드를 지정할 수 없습니다."),
    DEFAULT_GENERATION_REQUIRES_DEFAULT_QUESTION(HttpStatus.BAD_REQUEST, "D005", "생성 방식이 DEFAULT인 질문에는 기본 질문이 필요합니다."),
    AI_GENERATION_CANNOT_HAVE_DEFAULT_QUESTION(HttpStatus.BAD_REQUEST, "D006", "생성 방식이 AI인 질문에는 기본 질문을 지정할 수 없습니다.")

    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}