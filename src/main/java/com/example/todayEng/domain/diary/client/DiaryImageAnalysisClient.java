package com.example.todayEng.domain.diary.client;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DiaryImageAnalysisClient {

    DiaryImageAnalysis analyze(List<MultipartFile> images);
}
