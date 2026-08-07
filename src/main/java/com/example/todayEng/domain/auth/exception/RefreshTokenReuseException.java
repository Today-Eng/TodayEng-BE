package com.example.todayEng.domain.auth.exception;

import com.example.todayEng.global.error.ErrorCode;
import com.example.todayEng.global.error.exception.BaseException;

public class RefreshTokenReuseException extends BaseException {
    public RefreshTokenReuseException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
