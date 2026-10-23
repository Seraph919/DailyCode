package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeLinkChallengeDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LeetCodeLinkServiceTests {

    private final LeetCodeClient client = mock(LeetCodeClient.class);
    private final LeetCodeLinkTransaction linkTransaction = mock(LeetCodeLinkTransaction.class);
    private final LeetCodeAccountRepository accounts = mock(LeetCodeAccountRepository.class);
    private final LeetCodeLinkService service = new LeetCodeLinkService(client, linkTransaction, accounts);

    private final DiscordUser user = new DiscordUser("123456789012345678", "alice");

    @Test
    void startsLinkSuccessfully() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.empty());

        LeetCodeUserProfileDto profile = new LeetCodeUserProfileDto();
        profile.setUsername("alice_lc");
        when(client.fetchProfile("alice_lc")).thenReturn(profile);

        LeetCodeLinkChallengeDto expectedChallenge = new LeetCodeLinkChallengeDto("alice_lc", "dailycode-token123", Instant.now().plusSeconds(600));
        when(linkTransaction.createChallenge("123456789012345678", "alice_lc")).thenReturn(expectedChallenge);

        LeetCodeLinkChallengeDto result = service.startLink("123456789012345678", "alice_lc");
        assertThat(result).isSameAs(expectedChallenge);
        verify(linkTransaction).createChallenge("123456789012345678", "alice_lc");
    }

    @Test
    void throwsWhenAlreadyVerified() {
        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        Instant now = Instant.now();
        account.startVerification("token", now.plusSeconds(600), now);
        account.completeVerification("token", now.plusSeconds(60));

        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.startLink("123456789012345678", "alice_lc"))
                .isInstanceOf(LeetCodeLinkStateException.class)
                .extracting("reason")
                .isEqualTo(LeetCodeLinkStateException.Reason.ALREADY_VERIFIED);

        verifyNoInteractions(client);
        verifyNoInteractions(linkTransaction);
    }

    @Test
    void throwsWhenPendingLinkHasDifferentUsername() {
        LeetCodeAccount account = new LeetCodeAccount(user, "alice_lc");
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.startLink("123456789012345678", "bob_lc"))
                .isInstanceOf(LeetCodeLinkStateException.class)
                .extracting("reason")
                .isEqualTo(LeetCodeLinkStateException.Reason.DIFFERENT_ACCOUNT);

        verifyNoInteractions(client);
        verifyNoInteractions(linkTransaction);
    }

    @Test
    void throwsWhenUpstreamLeetCodeReturnsDifferentUsername() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.empty());

        LeetCodeUserProfileDto profile = new LeetCodeUserProfileDto();
        profile.setUsername("charlie_lc");
        when(client.fetchProfile("alice_lc")).thenReturn(profile);

        assertThatThrownBy(() -> service.startLink("123456789012345678", "alice_lc"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned a different username.");

        verifyNoInteractions(linkTransaction);
    }

    @Test
    void throwsWhenUpstreamLeetCodeReturnsInvalidUsername() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.empty());

        LeetCodeUserProfileDto profile = new LeetCodeUserProfileDto();
        profile.setUsername("   ");
        when(client.fetchProfile("alice_lc")).thenReturn(profile);

        assertThatThrownBy(() -> service.startLink("123456789012345678", "alice_lc"))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("LeetCode returned an invalid username.");

        verifyNoInteractions(linkTransaction);
    }
}
