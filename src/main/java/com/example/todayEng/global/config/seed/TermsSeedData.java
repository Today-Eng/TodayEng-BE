package com.example.todayEng.global.config.seed;

import com.example.todayEng.domain.user.entity.enums.TermsType;

public record TermsSeedData(TermsType type, String content) {
}
