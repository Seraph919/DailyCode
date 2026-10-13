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
            throw new IllegalStateException("No LeetCode account link exists for this Discord user.");
        }

        LeetCodeAccount account = existingAccount.get();
        Instant now = Instant.now();
        if (!proofChecker.hasValidProof(account, profile, now)) {
            throw new IllegalStateException("The profile does not prove ownership of an active challenge.");
        }

        account.completeVerification(expectedToken, now);
        // JPA saves changes to this managed entity when the transaction commits.
    }
}
