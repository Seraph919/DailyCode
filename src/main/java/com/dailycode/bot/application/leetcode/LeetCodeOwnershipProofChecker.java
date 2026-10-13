package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LeetCodeOwnershipProofChecker {

    public boolean hasValidProof(LeetCodeAccount account,
                                 LeetCodeVerificationProfileDto profile,
                                 Instant now) {
        if (account == null || now == null) {
            throw new IllegalArgumentException("Account and current time are required.");
        }
        if (!account.hasActiveVerificationChallenge(now)) {
            return false;
        }
        if (profile == null || profile.getUsername() == null || profile.getAboutMe() == null) {
            return false;
        }
        if (account.getLeetcodeUsername() == null
                || !account.getLeetcodeUsername().equalsIgnoreCase(profile.getUsername())) {
            return false;
        }

        // \\R splits the text at line breaks, including Windows and Unix line endings.
        String[] lines = profile.getAboutMe().split("\\R");
        for (String line : lines) {
            if (account.getVerificationToken().equals(line)) {
                return true;
            }
        }
        return false;
    }
}
