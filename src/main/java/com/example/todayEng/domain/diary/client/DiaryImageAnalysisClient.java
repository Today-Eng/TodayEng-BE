package com.example.todayEng.domain.diary.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DiaryImageAnalysisClient {

    JsonNode analyze(List<MultipartFile> images);
}
