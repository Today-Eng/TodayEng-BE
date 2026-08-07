package com.example.todayEng.domain.home.dto;

import java.time.LocalDate;
import java.util.List;

public record HomeDiaryDateResponse(
        Long diaryId,
        LocalDate diaryDate,
        String dayOfWeek,
        String diaryStatus,
        List<String> keywords,
        String questionText,
        String correctedText
) {}
