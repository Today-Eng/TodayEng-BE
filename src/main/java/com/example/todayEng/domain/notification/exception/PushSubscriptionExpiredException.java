package com.example.todayEng.domain.notification.exception;

public class PushSubscriptionExpiredException
        extends RuntimeException {

    public PushSubscriptionExpiredException() {
        super("푸시 구독이 만료되었습니다.");
    }
}
