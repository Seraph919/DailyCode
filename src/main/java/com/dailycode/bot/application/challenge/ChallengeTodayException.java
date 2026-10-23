package com.dailycode.bot.application.challenge;

import lombok.Getter;
import java.time.Instant;
import java.time.LocalDate;

@Getter
public class ChallengeTodayException extends RuntimeException {

    private final Reason reason;
    private final Schedule schedule;

    public ChallengeTodayException(Reason reason) {
        this(reason, null);
    }

    public ChallengeTodayException(Reason reason, Schedule schedule) {
        super(reason.name());
        this.reason = reason;
        this.schedule = schedule;
    }

    public record Schedule(LocalDate date, String timezone, Instant postingAt, boolean postingTimePassed) {}

    public enum Reason {
        CHALLENGE_NOT_FOUND,
        CHALLENGE_CANCELLED,
        CHALLENGE_COMPLETED,
        CHALLENGE_NOT_STARTED,
        CHALLENGE_ENDED,
        NO_ASSIGNMENT
    }
}
