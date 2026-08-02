package com.example.todayEng.domain.diary.dto.request;

import jakarta.validation.constraints.Size;

public record DiaryCompleteRequest(
        @Size(max = MAX_FINAL_MEMO_LENGTH, message = "finalMemo는 2000자 이하여야 합니다.")
        String finalMemo
) {
    // TODO: 기획에서 최대 길이를 확정하면 이 상수와 DB 컬럼 길이를 함께 변경한다.
    public static final int MAX_FINAL_MEMO_LENGTH = 2000;
}
