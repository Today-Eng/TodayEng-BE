package com.example.todayEng.domain.diary.dto.response;

import com.example.todayEng.domain.diary.entity.enums.DiaryStatus;
import com.example.todayEng.domain.diary.entity.enums.ReflectionQuestionGenerationStatus;
import java.util.List;

public record DiaryQuestionListResponse(
        Long diaryId,
        DiaryStatus diaryStatus,
        ReflectionQuestionGenerationStatus questionGenerationStatus,
        List<DiaryQuestionResponse> questions
) { }
