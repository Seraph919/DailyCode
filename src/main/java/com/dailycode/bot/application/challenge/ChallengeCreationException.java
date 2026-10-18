package com.dailycode.bot.application.challenge;

public class ChallengeCreationException extends RuntimeException {

    public enum Reason {
        INVALID_TIMEZONE,
        START_DATE_IN_PAST,
        END_DATE_BEFORE_START,
        USER_NOT_REGISTERED,
        ACCOUNT_NOT_VERIFIED
    }

    private final Reason reason;

    public ChallengeCreationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
