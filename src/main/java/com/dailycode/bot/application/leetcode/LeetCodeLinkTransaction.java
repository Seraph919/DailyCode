package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeLinkChallengeDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LeetCodeLinkTransaction {

    private static final Duration CHALLENGE_LIFETIME = Duration.ofMinutes(10);

    private final DiscordUserRepository users;
    private final LeetCodeAccountRepository accounts;
    private final VerificationTokenGenerator tokenGenerator;

    @Transactional
    public LeetCodeLinkChallengeDto createChallenge(String discordUserId, String leetcodeUsername) {
        // Lock the user even before an account exists, serializing first-link requests.
        Optional<DiscordUser> existingUser = users.findForLinkByDiscordUserId(discordUserId);
        if (existingUser.isEmpty()) {
            throw new IllegalStateException("Register the Discord user before linking a LeetCode account.");
        }
        // Share the account lock with verification, then evaluate expiry after waiting.
        Optional<LeetCodeAccount> existingAccount = accounts.findForVerificationByDiscordUserId(discordUserId);
        if (existingAccount.isPresent()) {
            LeetCodeAccount account = existingAccount.get();
            if (account.isVerified()) {
                throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.ALREADY_VERIFIED);
            }
            if (!account.getLeetcodeUsername().equalsIgnoreCase(leetcodeUsername)) {
                throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.DIFFERENT_ACCOUNT);
            }

            Instant now = Instant.now();
            if (!account.hasActiveVerificationChallenge(now)) {
                account.startVerification(tokenGenerator.generateToken(), now.plus(CHALLENGE_LIFETIME), now);
            }
            // A retry recovers an active token without rotating it or extending its deadline.
            // JPA persists a renewed challenge when this transaction commits.
            return toChallenge(account);
        }
        if (accounts.findByLeetcodeUsernameIgnoreCase(leetcodeUsername).isPresent()) {
            throw new LeetCodeLinkStateException(LeetCodeLinkStateException.Reason.USERNAME_RESERVED);
        }

        Instant now = Instant.now();
        String token = tokenGenerator.generateToken();
        LeetCodeAccount account = new LeetCodeAccount(existingUser.get(), leetcodeUsername);
        account.startVerification(token, now.plus(CHALLENGE_LIFETIME), now);
        // The database constraints also protect against simultaneous requests.
        accounts.saveAndFlush(account);

        return toChallenge(account);
    }

    private LeetCodeLinkChallengeDto toChallenge(LeetCodeAccount account) {
        return new LeetCodeLinkChallengeDto(account.getLeetcodeUsername(),
                account.getVerificationToken(), account.getVerificationExpiresAt());
    }
}
