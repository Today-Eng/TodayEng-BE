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
    INVALID_CALENDAR_DATE(HttpStatus.BAD_REQUEST, "C011", "조회할 연도와 월을 확인해주세요."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "C012", "지원하지 않는 Content-Type입니다."),


    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A004", "접근 권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A005", "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A006", "헤더에 토큰이 존재하지 않습니다."),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "A007", "유효하지 않은 Google ID 토큰입니다."),


    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않는 사용자입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U003", "이미 사용중인 닉네임입니다."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "U007", "존재하지 않는 약관입니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "U005", "필수 약관에 동의해야 합니다."),
    INTEREST_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "U006", "존재하지 않는 관심사 태그입니다."),
    EXTERNAL_ACCOUNT_NOT_CONNECTED(HttpStatus.NOT_FOUND, "U004", "연동되지 않은 외부 서비스입니다."),


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
    OAUTH_TOKEN_EXCHANGE_FAILED(HttpStatus.BAD_GATEWAY, "E010", "OAuth 토큰 발급에 실패했습니다."),
    OAUTH_USER_INFO_FAILED(HttpStatus.BAD_GATEWAY, "E011", "외부 계정 정보를 조회하지 못했습니다."),
    EXTERNAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "E012", "이미 다른 사용자에게 연동된 외부 계정입니다."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "E013", "지원하지 않는 외부 서비스 provider입니다."),
    OAUTH_STATE_CONSUME_FAILED(HttpStatus.CONFLICT, "E014", "OAuth 인증 요청 처리에 실패했습니다."),
    OAUTH_AUTHORIZATION_CODE_MISSING(HttpStatus.BAD_REQUEST, "E015", "OAuth Authorization Code가 존재하지 않습니다."),

    // Diary (D)
    MAIN_QUESTION_CANNOT_HAVE_PARENT(HttpStatus.BAD_REQUEST, "D001", "메인 질문에는 상위 질문을 지정할 수 없습니다."),
    FOLLOW_UP_QUESTION_REQUIRES_PARENT(HttpStatus.BAD_REQUEST, "D002", "후속 질문에는 메인 질문이 필요합니다."),
    FOLLOW_UP_PARENT_MUST_BE_MAIN_QUESTION(HttpStatus.BAD_REQUEST,"D003","후속 질문의 상위 질문은 메인 질문이어야 합니다."),
    FOLLOW_UP_QUESTION_CANNOT_HAVE_KEYWORD(HttpStatus.BAD_REQUEST, "D004", "후속 질문에는 키워드를 지정할 수 없습니다."),
    DEFAULT_GENERATION_REQUIRES_DEFAULT_QUESTION(HttpStatus.BAD_REQUEST, "D005", "생성 방식이 DEFAULT인 질문에는 기본 질문이 필요합니다."),
    AI_GENERATION_CANNOT_HAVE_DEFAULT_QUESTION(HttpStatus.BAD_REQUEST, "D006", "생성 방식이 AI인 질문에는 기본 질문을 지정할 수 없습니다."),
    FUTURE_DIARY_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "D007", "미래 날짜에는 회고를 작성할 수 없습니다."),
    DIARY_DATE_EXPIRED(HttpStatus.BAD_REQUEST, "D008", "7일이 지난 날짜에는 회고를 작성할 수 없습니다."),
    DIARY_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "D009", "해당 날짜에 이미 진행 중인 회고가 있습니다."),
    DIARY_ALREADY_COMPLETED(HttpStatus.CONFLICT, "D010", "해당 날짜의 회고가 이미 완료되었습니다."),
    DIARY_NOT_FOUND(HttpStatus.NOT_FOUND, "D011", "존재하지 않는 회고입니다."),
    DIARY_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "D012", "진행 가능한 회고 상태가 아닙니다."),
    REFLECTION_QUESTIONS_ALREADY_GENERATED(HttpStatus.CONFLICT, "D013", "회고 질문이 이미 생성되었거나 생성 중입니다."),
    DIARY_CONTEXT_NOT_FOUND(HttpStatus.BAD_REQUEST, "D014", "질문 생성에 사용할 회고 컨텍스트가 없습니다."),
    INVALID_LLM_RESPONSE(HttpStatus.BAD_GATEWAY, "D015", "LLM 응답 형식이 올바르지 않습니다."),
    LLM_API_FAILED(HttpStatus.BAD_GATEWAY, "D016", "LLM API 호출에 실패했습니다."),
    INVALID_QUESTION_CONTEXT(HttpStatus.BAD_GATEWAY, "D017", "LLM이 허용되지 않은 컨텍스트를 선택했습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "D018", "존재하지 않는 질문입니다."),
    QUESTION_TTS_ALREADY_PROCESSING(HttpStatus.CONFLICT, "D019", "질문 음성이 이미 생성되었거나 생성 중입니다."),
    TTS_API_FAILED(HttpStatus.BAD_GATEWAY, "D020", "Google TTS 호출에 실패했습니다."),
    AUDIO_STORAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "D021", "질문 음성 저장에 실패했습니다."),
    QUESTION_NOT_ANSWERABLE(HttpStatus.CONFLICT, "D022", "현재 답변할 수 있는 질문이 아닙니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "D023", "이미 답변이 등록된 질문입니다."),
    INVALID_AUDIO_FILE(HttpStatus.BAD_REQUEST, "D024", "지원하지 않거나 올바르지 않은 음성 파일입니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "D025", "존재하지 않는 답변입니다."),
    STT_API_FAILED(HttpStatus.BAD_GATEWAY, "D026", "Google STT 호출에 실패했습니다."),
    INVALID_STT_RESPONSE(HttpStatus.BAD_GATEWAY, "D027", "STT 결과가 비어 있거나 올바르지 않습니다."),
    ANSWER_CORRECTION_ALREADY_PROCESSING(HttpStatus.CONFLICT, "D028", "답변 교정이 이미 완료되었거나 처리 중입니다."),
    FOLLOW_UP_QUESTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "D029", "후속 질문이 이미 존재합니다."),
    INVALID_REFLECTION_QUESTION_COMPOSITION(HttpStatus.CONFLICT, "D030", "회고 질문 구성이 완료 조건을 충족하지 않습니다."),
    INCOMPLETE_REFLECTION_ANSWERS(HttpStatus.CONFLICT, "D031", "모든 회고 답변이 완료되지 않았습니다."),
    REFLECTION_CORRECTION_NOT_FINISHED(HttpStatus.CONFLICT, "D032", "답변 교정 처리가 완료되지 않았습니다."),
    DIARY_ALREADY_PAUSED(HttpStatus.CONFLICT, "D033", "이미 중단된 회고입니다."),
    INVALID_DIARY_YEAR_MONTH(HttpStatus.BAD_REQUEST, "D034", "조회할 연도 또는 월이 올바르지 않습니다."),
    DIARY_NOT_COMPLETED(HttpStatus.CONFLICT, "D035", "완료된 회고만 메모를 수정할 수 있습니다."),
    DIARY_CONTEXT_ALREADY_GENERATED(HttpStatus.CONFLICT, "D036", "회고 컨텍스트가 이미 생성되었거나 생성 중입니다."),
    DIARY_ALREADY_DELETED(HttpStatus.CONFLICT, "D037", "이미 삭제된 회고입니다."),
    DIARY_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "D038", "완료된 회고만 삭제할 수 있습니다."),
    REFLECTION_QUESTION_CLAIM_LOST(HttpStatus.CONFLICT, "D039", "질문 생성 작업이 다른 요청으로 회수되었습니다."),
    TOO_MANY_DIARY_IMAGES(HttpStatus.BAD_REQUEST, "D040", "사진은 최대 2장까지 첨부할 수 있습니다."),
    DIARY_MEMO_TOO_LONG(HttpStatus.BAD_REQUEST, "D041", "메모는 최대 200자까지 입력할 수 있습니다."),
    INVALID_DIARY_LOCATION(HttpStatus.BAD_REQUEST, "D042", "위도 또는 경도 값이 올바르지 않습니다."),


    // Notification (N)
    PUSH_SUBSCRIPTION_REQUIRED(HttpStatus.BAD_REQUEST, "N001", "푸시 구독 정보가 없어 알림을 활성화할 수 없습니다."),
    INVALID_PUSH_SUBSCRIPTION(HttpStatus.BAD_REQUEST, "N002", "푸시 구독 정보는 모두 입력되어야 합니다."),
    NOTIFICATION_DISABLED(HttpStatus.BAD_REQUEST, "N003", "알림이 비활성화되어 있습니다."),
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
