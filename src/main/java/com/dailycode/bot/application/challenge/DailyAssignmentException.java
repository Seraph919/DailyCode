package com.dailycode.bot.application.challenge;

import lombok.Getter;

@Getter
public class DailyAssignmentException extends RuntimeException {

    private final Reason reason;

    public DailyAssignmentException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public enum Reason {
        CHALLENGE_NOT_FOUND,
        CHALLENGE_CANCELLED,
        CHALLENGE_COMPLETED,
        CHALLENGE_NOT_STARTED,
        CHALLENGE_ENDED,
        NO_ELIGIBLE_PROBLEM,
        DAY_CHANGED
    }
}
