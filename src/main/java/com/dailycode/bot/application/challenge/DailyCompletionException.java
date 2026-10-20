package com.dailycode.bot.application.challenge;

import lombok.Getter;

@Getter
public class DailyCompletionException extends RuntimeException {
    private final Reason reason;

    public DailyCompletionException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public enum Reason {
        CHALLENGE_NOT_FOUND,
        CHALLENGE_CANCELLED,
        DATE_OUTSIDE_CHALLENGE,
        DAY_NOT_STARTED,
        NOT_JOINED,
        ACCOUNT_NOT_VERIFIED,
        NO_ASSIGNMENT,
        NOT_ELIGIBLE_FOR_DAY,
        STATE_CHANGED
    }
}
