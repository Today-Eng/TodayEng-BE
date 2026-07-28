package com.example.todayEng.domain.user.entity.enums;

public enum InterestCategory {

    ENTERTAINMENT_ART(
            "엔터테인먼트 / 예술",
            1
    ),

    LIFE_SHOPPING(
            "생활 / 노하우 / 쇼핑",
            2
    ),

    HOBBY_TRAVEL(
            "취미 / 여가 / 여행",
            3
    );

    private final String displayName;
    private final int displayOrder;

    InterestCategory(
            String displayName,
            int displayOrder
    ) {
        this.displayName = displayName;
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}