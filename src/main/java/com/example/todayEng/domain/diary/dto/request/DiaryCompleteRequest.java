package com.example.todayEng.domain.diary.dto.request;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일기 최종 저장 요청")
public record DiaryCompleteRequest(
        @Schema(description = "최종 회고 메모. null 허용, 공백만 입력하면 null로 저장", example = "오늘 친구와 전시회를 다녀온 일이 가장 기억에 남는다.", maxLength = 2000, nullable = true)
        @Size(max = MAX_FINAL_MEMO_LENGTH, message = "finalMemo는 2000자 이하여야 합니다.")
        String finalMemo
) {
    // TODO: 기획에서 최대 길이를 확정하면 이 상수와 DB 컬럼 길이를 함께 변경한다.
    public static final int MAX_FINAL_MEMO_LENGTH = 2000;
}
