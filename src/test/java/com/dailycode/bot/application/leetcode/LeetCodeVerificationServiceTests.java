package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LeetCodeVerificationServiceTests {

    private final LeetCodeAccountRepository accounts = mock(LeetCodeAccountRepository.class);
    private final LeetCodeClient client = mock(LeetCodeClient.class);
    private final LeetCodeVerificationTransaction verificationTransaction = mock(LeetCodeVerificationTransaction.class);
    private final LeetCodeVerificationService service = new LeetCodeVerificationService(accounts, client, verificationTransaction);

    private final DiscordUser user = new DiscordUser("123456789012345678", "alice");

    @Test
    void verifiesAccountSuccessfully() {
        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        Instant now = Instant.now();
        account.startVerification("dailycode-token123", now.plus(10, ChronoUnit.MINUTES), now);

        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));
        var profile = new LeetCodeVerificationProfileDto("alice_lc", "dailycode-token123");
        when(client.fetchVerificationProfile("alice_lc")).thenReturn(profile);

        service.verify("123456789012345678");

        verify(verificationTransaction).completeVerification("123456789012345678", "dailycode-token123", profile);
    }

    @Test
    void throwsWhenNoLinkExists() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("123456789012345678"))
                .isInstanceOf(LeetCodeLinkStateException.class)
                .extracting("reason")
                .isEqualTo(LeetCodeLinkStateException.Reason.NO_LINK);

        verifyNoInteractions(client);
        verifyNoInteractions(verificationTransaction);
    }

    @Test
    void throwsWhenAlreadyVerified() {
        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        Instant now = Instant.now();
        account.startVerification("token", now.plusSeconds(600), now);
        account.completeVerification("token", now.plusSeconds(60));

        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.verify("123456789012345678"))
                .isInstanceOf(LeetCodeLinkStateException.class)
                .extracting("reason")
                .isEqualTo(LeetCodeLinkStateException.Reason.ALREADY_VERIFIED);

        verifyNoInteractions(client);
        verifyNoInteractions(verificationTransaction);
    }

    @Test
    void throwsWhenChallengeInactiveOrExpired() {
        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        // No challenge started
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.verify("123456789012345678"))
                .isInstanceOf(LeetCodeLinkStateException.class)
                .extracting("reason")
                .isEqualTo(LeetCodeLinkStateException.Reason.INACTIVE_CHALLENGE);

        verifyNoInteractions(client);
        verifyNoInteractions(verificationTransaction);
    }
}
