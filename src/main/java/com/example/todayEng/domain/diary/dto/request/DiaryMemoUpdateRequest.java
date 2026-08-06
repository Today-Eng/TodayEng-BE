package com.example.todayEng.domain.diary.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "회고 메모 수정 요청")
public record DiaryMemoUpdateRequest(
        @Schema(
                description = "수정할 메모. null 허용, 공백만 입력하면 null로 저장",
                example = "오늘 공연이 너무 재밌었다.",
                maxLength = MAX_MEMO_LENGTH,
                nullable = true
        )
        @Size(max = MAX_MEMO_LENGTH, message = "memo는 2000자 이하여야 합니다.")
        String memo
) {
    public static final int MAX_MEMO_LENGTH = 2000;
}
