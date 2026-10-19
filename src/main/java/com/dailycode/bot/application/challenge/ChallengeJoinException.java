package com.dailycode.bot.application.challenge;

public class ChallengeJoinException extends RuntimeException {

    public enum Reason {
        CHALLENGE_NOT_FOUND,
        ALREADY_JOINED,
        CHALLENGE_CANCELLED,
        CHALLENGE_COMPLETED,
        CHALLENGE_ENDED,
        USER_NOT_REGISTERED,
        ACCOUNT_NOT_VERIFIED
    }

    private final Reason reason;

    public ChallengeJoinException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
