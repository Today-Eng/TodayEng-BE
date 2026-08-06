package com.example.todayEng.domain.diary.client;

import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisCommand;
import com.example.todayEng.domain.diary.dto.llm.DiaryMemoryAnalysisResponse;

public interface DiaryMemoryAnalysisClient {

    DiaryMemoryAnalysisResponse analyze(DiaryMemoryAnalysisCommand command);
}
