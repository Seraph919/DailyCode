package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeetCodeVerificationTransaction {

    private final LeetCodeAccountRepository accounts;
    private final LeetCodeOwnershipProofChecker proofChecker;

    @Transactional
    public void completeVerification(String discordUserId, String expectedToken,
                                     LeetCodeVerificationProfileDto profile) {
        if (discordUserId == null || discordUserId.isBlank()) {
            throw new IllegalArgumentException("Discord user ID is required.");
        }
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new IllegalArgumentException("The checked verification token is required.");
        }

        Optional<LeetCodeAccount> existingAccount = accounts.findForVerificationByDiscordUserId(discordUserId);
        if (existingAccount.isEmpty()) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.NO_LINK);
        }

        LeetCodeAccount account = existingAccount.get();
        if (account.isVerified()) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.ALREADY_VERIFIED);
        }
        Instant now = Instant.now();
        if (!account.hasActiveVerificationChallenge(now)) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.INACTIVE_CHALLENGE);
        }
        if (!expectedToken.equals(account.getVerificationToken())) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.CHALLENGE_CHANGED);
        }
        if (!proofChecker.hasValidProof(account, profile, now)) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.PROOF_MISMATCH);
        }

        account.completeVerification(expectedToken, now);
        // JPA saves changes to this managed entity when the transaction commits.
    }
}
