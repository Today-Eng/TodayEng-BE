package com.example.todayEng.global.config.seed;

import java.util.List;

import static com.example.todayEng.domain.user.entity.enums.TermsType.*;

public final class TermsSeedCatalog {

    private static final List<TermsSeedData> VALUES = List.of(
            new TermsSeedData(
                    SERVICE_USE,
                    "서비스 이용을 위한 기본 약관에 동의합니다. 약관에는 서비스 이용 조건, 회원의 권리와 의무, "
                            + "서비스 이용 제한 사유 등이 포함되어 있으며, 전문은 [약관 전문 링크]에서 확인하실 수 있습니다."
            ),
            new TermsSeedData(
                    PRIVACY_COLLECTION,
                    "회원가입 및 서비스 제공을 위해 이메일, 비밀번호(또는 소셜 로그인 식별값), 닉네임, 그리고 회고 작성 과정에서 "
                            + "생성되는 회고 내용과 질문에 대한 답변을 수집합니다. 수집한 정보는 본인 확인, 회원자격 유지·관리, "
                            + "회고 작성 지원(맞춤 질문 생성 및 표현 교정)을 위한 목적으로만 이용되며, 회원 탈퇴 시까지 보유한 뒤 "
                            + "파기됩니다. 동의를 거부할 권리가 있으나, 동의하지 않으실 경우 회원가입 및 서비스 이용이 제한됩니다."
            ),
            new TermsSeedData(
                    AGE_OVER_FOURTEEN,
                    "만 14세 미만인 경우 법정대리인의 동의가 필요하며, 본 서비스는 만 14세 이상 이용자를 대상으로 합니다."
            ),
            new TermsSeedData(
                    AI_PROCESSING_AND_OVERSEAS_TRANSFER,
                    "회고 작성 시 입력하신 내용은 맞춤 질문 생성, 표현 교정, 과거 회고 분석을 위해 [LLM 업체명]의 서버"
                            + "(국가: [국가명])로 전송되어 처리됩니다. 이는 서비스의 핵심 기능 제공을 위해 필요한 처리이며, "
                            + "동의하지 않으실 경우 회원가입 및 서비스 이용이 제한됩니다."
            ),
            new TermsSeedData(
                    CALENDAR_INFORMATION_COLLECTION,
                    "캘린더 연동 기능을 이용하시면 연동된 캘린더의 일정 정보를 수집하여 회고 작성 시 관련 일정을 참고 자료로 "
                            + "제공해 드립니다. 동의하지 않으셔도 서비스 이용에 제한이 없으며, 캘린더 연동 기능만 이용하실 수 "
                            + "없습니다. 보유 기간은 연동 해제 또는 회원 탈퇴 시까지입니다."
            ),
            new TermsSeedData(
                    SPOTIFY_INFORMATION_COLLECTION,
                    "스포티파이 계정 연동 시 재생 기록, 플레이리스트 등 [실제 가져오는 데이터 항목]을 수집하여 "
                            + "[실제 이용 목적 — 예: AI 대화 콘텐츠 추천, 학습 콘텐츠 매칭 등]에 활용합니다. 동의하지 않으셔도 "
                            + "서비스 이용에 제한이 없으며, 스포티파이 연동 기능만 이용하실 수 없습니다. 보유 기간은 연동 해제 "
                            + "또는 회원 탈퇴 시까지입니다."
            ),
            new TermsSeedData(
                    LOCATION_INFORMATION_COLLECTION,
                    "회고 작성 시 [실제 목적 — 예: 방문한 장소 자동 태깅, 주변 장소 추천 등]을 위해 기기의 위치정보(GPS)를 "
                            + "수집·이용합니다. 위치정보는 회고 작성 시점에 일시적으로 수집되며, [보유 기간 — 예: 회고에 첨부된 "
                            + "위치 정보는 해당 회고 삭제 시 또는 회원 탈퇴 시까지 보유]됩니다. 동의하지 않으셔도 서비스 이용에 "
                            + "제한이 없으며, 위치 기반 기능만 이용하실 수 없습니다. 위치정보는 「위치정보의 보호 및 이용 등에 관한 "
                            + "법률」에 따라 별도로 관리되며, 만 14세 미만 아동의 경우 법정대리인의 동의가 추가로 필요할 수 있습니다."
            ),
            new TermsSeedData(
                    PHOTO_EXIF_LOCATION_COLLECTION,
                    "회고에 사진을 첨부하시는 경우, 사진 파일에 포함된 EXIF 메타데이터 중 촬영 위치(GPS 좌표) 정보를 추출하여 "
                            + "[실제 목적 — 예: 회고 작성 시 촬영 장소 자동 표시, 장소 기반 회고 정리 등]에 활용합니다. 동의하지 "
                            + "않으실 경우 위치 정보는 자동으로 제거된 상태로 사진이 저장되며, 서비스 이용에는 제한이 없습니다. "
                            + "보유 기간은 해당 사진(회고) 삭제 시 또는 회원 탈퇴 시까지입니다."
            ),
            new TermsSeedData(
                    MARKETING_INFORMATION_RECEIVE,
                    "이벤트, 신규 기능 안내 등 마케팅 목적의 푸시 알림 및 이메일을 받아보실 수 있습니다. 동의하지 않으셔도 "
                            + "서비스 이용에 제한이 없으며, 마이페이지에서 언제든지 수신 설정을 변경하실 수 있습니다."
            )
    );

    private TermsSeedCatalog() {
    }

    public static List<TermsSeedData> values() {
        return VALUES;
    }
}
