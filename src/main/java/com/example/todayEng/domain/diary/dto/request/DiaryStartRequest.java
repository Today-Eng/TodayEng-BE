package com.example.todayEng.domain.diary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DiaryStartRequest(

        @Schema(description = "회고 날짜", example = "2026-07-28")
        @NotNull(message = "회고 날짜는 필수입니다.")
        LocalDate diaryDate

) {
}
