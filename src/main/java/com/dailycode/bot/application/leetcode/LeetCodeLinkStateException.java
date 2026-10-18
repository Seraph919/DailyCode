package com.dailycode.bot.application.leetcode;

public class LeetCodeLinkStateException extends IllegalStateException {

    public enum Reason {
        ALREADY_VERIFIED,
        NO_LINK,
        INACTIVE_CHALLENGE,
        DIFFERENT_ACCOUNT,
        USERNAME_RESERVED,
        CHALLENGE_CHANGED,
        PROOF_MISMATCH
    }

    private final Reason reason;

    public LeetCodeLinkStateException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
