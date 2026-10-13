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
        Optional<DiscordUser> existingUser = users.findByDiscordUserId(discordUserId);
        if (existingUser.isEmpty()) {
            throw new IllegalStateException("Register the Discord user before linking a LeetCode account.");
        }
        if (accounts.findByDiscordUser_DiscordUserId(discordUserId).isPresent()) {
            throw new IllegalStateException("This Discord user already has a pending or verified account link.");
        }
        if (accounts.findByLeetcodeUsernameIgnoreCase(leetcodeUsername).isPresent()) {
            throw new IllegalStateException("This LeetCode username is already reserved by an account link.");
        }

        Instant now = Instant.now();
        String token = tokenGenerator.generateToken();
        LeetCodeAccount account = new LeetCodeAccount(existingUser.get(), leetcodeUsername);
        account.startVerification(token, now.plus(CHALLENGE_LIFETIME), now);
        // The database constraints also protect against simultaneous requests.
        accounts.saveAndFlush(account);

        return new LeetCodeLinkChallengeDto(account.getLeetcodeUsername(),
                account.getVerificationToken(), account.getVerificationExpiresAt());
    }
}
