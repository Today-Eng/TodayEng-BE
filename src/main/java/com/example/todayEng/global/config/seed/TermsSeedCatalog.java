package com.example.todayEng.global.config.seed;

import java.util.List;

import static com.example.todayEng.domain.user.entity.enums.TermsType.*;

public final class TermsSeedCatalog {

    private static final int CURRENT_VERSION = 2;

    private static final List<TermsSeedData> VALUES = List.of(
            new TermsSeedData(SERVICE_USE, CURRENT_VERSION,
                    "서비스 이용을 위한 기본 약관에 동의합니다. 약관에는 서비스 이용 조건, 회원의 권리와 의무, "
                            + "서비스 이용 제한 사유 등이 포함됩니다."),
            new TermsSeedData(PRIVACY_COLLECTION, CURRENT_VERSION,
                    "회원가입 및 서비스 제공을 위해 이메일, 비밀번호(또는 소셜 로그인 식별값), 닉네임, 그리고 회고 작성 과정에서 "
                            + "생성되는 회고 내용과 질문에 대한 답변을 수집합니다. 수집한 정보는 본인 확인, 회원자격 유지·관리, "
                            + "회고 작성 지원(맞춤 질문 생성 및 표현 교정)을 위한 목적으로만 이용되며, 회원 탈퇴 시까지 보유한 뒤 "
                            + "파기됩니다. 동의를 거부할 권리가 있으나, 동의하지 않으실 경우 회원가입 및 서비스 이용이 제한됩니다."),
            new TermsSeedData(AGE_OVER_FOURTEEN, CURRENT_VERSION,
                    "만 14세 미만인 경우 법정대리인의 동의가 필요하며, 본 서비스는 만 14세 이상 이용자를 대상으로 합니다."),
            new TermsSeedData(AI_PROCESSING_AND_OVERSEAS_TRANSFER, CURRENT_VERSION,
                    "회고 작성 시 입력한 회고 내용, 질문 답변 및 첨부 사진은 맞춤 질문 생성, 영어 표현 교정, 과거 회고 분석과 "
                            + "사진 맥락 분석을 위해 Google LLC가 제공하는 Gemini API로 전송됩니다. 해당 정보는 미국을 포함하여 "
                            + "Google이 데이터 처리 시설을 운영하는 국가에서 처리될 수 있습니다. 이는 서비스 핵심 기능 제공에 "
                            + "필요하며, 동의하지 않으면 회원가입 및 서비스 이용이 제한됩니다."),
            new TermsSeedData(CALENDAR_INFORMATION_COLLECTION, CURRENT_VERSION,
                    "Google 캘린더 연동 시 일정의 제목, 시작·종료 시각 등 일정 정보를 조회하여 회고 작성 시 참고 자료와 맞춤 질문 "
                            + "생성에 이용합니다. 동의하지 않아도 기본 서비스 이용에는 제한이 없으며 캘린더 연동 기능만 이용할 수 "
                            + "없습니다. 수집된 회고 참고 정보는 해당 회고 삭제 또는 회원 탈퇴 시까지 보유합니다."),
            new TermsSeedData(SPOTIFY_INFORMATION_COLLECTION, CURRENT_VERSION,
                    "Spotify 계정 연동 시 최근 재생한 트랙의 곡명, 아티스트 및 재생 시각을 조회하여 회고 작성 참고 자료와 맞춤 질문 "
                            + "생성에 이용합니다. 동의하지 않아도 기본 서비스 이용에는 제한이 없으며 Spotify 연동 기능만 이용할 수 "
                            + "없습니다. 수집된 회고 참고 정보는 해당 회고 삭제 또는 회원 탈퇴 시까지 보유합니다."),
            new TermsSeedData(LOCATION_INFORMATION_COLLECTION, CURRENT_VERSION,
                    "회고 작성 시 사용자가 제공한 기기의 위도·경도 정보를 날씨 조회와 장소 관련 회고 참고 정보 생성에 이용합니다. "
                            + "동의하지 않아도 기본 서비스 이용에는 제한이 없으며 위치 기반 기능만 이용할 수 없습니다. 생성된 회고 "
                            + "참고 정보는 해당 회고 삭제 또는 회원 탈퇴 시까지 보유합니다."),
            new TermsSeedData(PHOTO_EXIF_LOCATION_COLLECTION, CURRENT_VERSION,
                    "회고에 사진을 첨부하면 원본 사진에 포함된 촬영 위치 등 EXIF 메타데이터가 사진과 함께 Google Gemini API로 "
                            + "전송될 수 있으며, 사진 맥락을 분석해 회고 참고 정보와 맞춤 질문을 생성하는 데 이용됩니다. 동의하지 "
                            + "않아도 기본 서비스 이용에는 제한이 없으며 사진 기반 기능만 이용할 수 없습니다. 서버에는 원본 사진을 "
                            + "별도로 저장하지 않고 분석 결과를 해당 회고 삭제 또는 회원 탈퇴 시까지 보유합니다."),
            new TermsSeedData(MARKETING_INFORMATION_RECEIVE, CURRENT_VERSION,
                    "이벤트, 신규 기능 안내 등 마케팅 목적의 푸시 알림 및 이메일을 받아볼 수 있습니다. 동의하지 않아도 서비스 "
                            + "이용에는 제한이 없으며, 마이페이지에서 언제든지 수신 설정을 변경할 수 있습니다.")
    );

    private TermsSeedCatalog() {
    }

    public static List<TermsSeedData> values() {
        return VALUES;
    }
}
