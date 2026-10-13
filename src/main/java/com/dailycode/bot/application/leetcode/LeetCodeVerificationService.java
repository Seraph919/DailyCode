package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class LeetCodeVerificationService {

    private final LeetCodeAccountRepository accounts;
    private final LeetCodeClient client;
    private final LeetCodeVerificationTransaction verificationTransaction;

    @Transactional(propagation = Propagation.NEVER)
    public void verify(@NotBlank @Size(max = 20) String discordUserId) {
        Optional<LeetCodeAccount> existingAccount = accounts.findByDiscordUser_DiscordUserId(discordUserId);
        if (existingAccount.isEmpty()) {
            throw new IllegalStateException("No LeetCode account link exists for this Discord user.");
        }

        LeetCodeAccount account = existingAccount.get();
        if (!account.hasActiveVerificationChallenge(Instant.now())) {
            throw new IllegalStateException("Account has no active verification challenge.");
        }

        String username = account.getLeetcodeUsername();
        String expectedToken = account.getVerificationToken();
        LeetCodeVerificationProfileDto profile = client.fetchVerificationProfile(username);

        // The transaction reloads the account and checks the challenge again under a lock.
        verificationTransaction.completeVerification(discordUserId, expectedToken, profile);
    }
}
