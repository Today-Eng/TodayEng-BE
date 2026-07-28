package com.example.todayEng.domain.user.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TermsType {

    SERVICE_USE(
            "서비스 이용약관 동의",
            true,
            1
    ),

    PRIVACY_COLLECTION(
            "개인정보 수집 및 이용 동의",
            true,
            2
    ),

    AGE_OVER_FOURTEEN(
            "만 14세 이상입니다.",
            true,
            3
    ),

    AI_PROCESSING_AND_OVERSEAS_TRANSFER(
            "회고 내용의 AI 처리 및 국외이전 동의",
            true,
            4
    ),

    CALENDAR_INFORMATION_COLLECTION(
            "캘린더 연동을 위한 일정 정보 수집 동의",
            false,
            5
    ),

    SPOTIFY_INFORMATION_COLLECTION(
            "스포티파이 연동을 위한 정보 수집 동의",
            false,
            6
    ),

    LOCATION_INFORMATION_COLLECTION(
            "위치정보 수집 및 이용 동의",
            false,
            7
    ),

    PHOTO_EXIF_LOCATION_COLLECTION(
            "사진 EXIF 위치정보(GPS) 수집 및 이용 동의",
            false,
            8
    ),

    MARKETING_INFORMATION_RECEIVE(
            "마케팅 정보 수신 동의",
            false,
            9
    );

    private final String displayName;
    private final boolean required;
    private final int displayOrder;
}