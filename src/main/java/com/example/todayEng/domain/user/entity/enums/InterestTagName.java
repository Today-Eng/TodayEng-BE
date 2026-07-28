package com.example.todayEng.domain.user.entity.enums;


public enum InterestTagName {

    // 엔터테인먼트 / 예술
    CULTURE_BOOK("문화/책", InterestCategory.ENTERTAINMENT_ART, 1),
    MOVIE("영화", InterestCategory.ENTERTAINMENT_ART, 2),
    ART_DESIGN("미술/디자인", InterestCategory.ENTERTAINMENT_ART, 3),
    PERFORMANCE_EXHIBITION("공연/전시", InterestCategory.ENTERTAINMENT_ART, 4),
    MUSIC("음악", InterestCategory.ENTERTAINMENT_ART, 5),
    DRAMA("드라마", InterestCategory.ENTERTAINMENT_ART, 6),
    CELEBRITY("스타/연예인", InterestCategory.ENTERTAINMENT_ART, 7),
    CARTOON_ANIMATION("만화/애니", InterestCategory.ENTERTAINMENT_ART, 8),
    BROADCAST("방송", InterestCategory.ENTERTAINMENT_ART, 9),

    // 생활 / 노하우 / 쇼핑
    DAILY_THOUGHT("일상/생각", InterestCategory.LIFE_SHOPPING, 1),
    PARENTING_MARRIAGE("육아/결혼", InterestCategory.LIFE_SHOPPING, 2),
    PET("애완/반려동물", InterestCategory.LIFE_SHOPPING, 3),
    GOOD_WRITING_IMAGE("좋은글/이미지", InterestCategory.LIFE_SHOPPING, 4),
    FASHION_BEAUTY("패션/미용", InterestCategory.LIFE_SHOPPING, 5),
    INTERIOR_DIY("인테리어/DIY", InterestCategory.LIFE_SHOPPING, 6),
    GARDENING("원예/재배", InterestCategory.LIFE_SHOPPING, 7),
    PRODUCT_REVIEW("상품리뷰", InterestCategory.LIFE_SHOPPING, 8),
    COOKING_RECIPE("요리/레시피", InterestCategory.LIFE_SHOPPING, 9),

    // 취미 / 여가 / 여행
    GAME("게임", InterestCategory.HOBBY_TRAVEL, 1),
    SPORTS("스포츠", InterestCategory.HOBBY_TRAVEL, 2),
    PHOTOGRAPHY("사진", InterestCategory.HOBBY_TRAVEL, 3),
    CAR("자동차", InterestCategory.HOBBY_TRAVEL, 4),
    HOBBY("취미", InterestCategory.HOBBY_TRAVEL, 5),
    DOMESTIC_TRAVEL("국내여행", InterestCategory.HOBBY_TRAVEL, 6),
    INTERNATIONAL_TRAVEL("세계여행", InterestCategory.HOBBY_TRAVEL, 7),
    RESTAURANT("맛집", InterestCategory.HOBBY_TRAVEL, 8);

    private final String displayName;
    private final InterestCategory category;
    private final int displayOrder;

    InterestTagName(
            String displayName,
            InterestCategory category,
            int displayOrder
    ) {
        this.displayName = displayName;
        this.category = category;
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public InterestCategory getCategory() {
        return category;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
