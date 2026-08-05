package com.example.todayEng.domain.diary.dto.response;

import java.util.List;

public record DiaryAnswerListResponse(long answeredCount, long expectedAnswerCount,
        List<DiaryAnswerSummaryResponse> answers) { }
