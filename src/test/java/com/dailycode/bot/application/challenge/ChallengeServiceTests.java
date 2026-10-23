package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DiscordUserRepository;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.dailycode.bot.application.challenge.ChallengeCreationException.Reason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChallengeServiceTests {

    private final DiscordUserRepository users = mock(DiscordUserRepository.class);
    private final LeetCodeAccountRepository accounts = mock(LeetCodeAccountRepository.class);
    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));

    private final ChallengeService service = new ChallengeService(users, accounts, challenges, clock);

    private final DiscordUser creator = new DiscordUser("123456789012345678", "alice");

    @BeforeEach
    void setUp() {
        when(users.findByDiscordUserId("123456789012345678")).thenReturn(Optional.of(creator));
        LeetCodeAccount account = new LeetCodeAccount(creator, "alice_lc");
        Instant now = Instant.now();
        account.startVerification("token", now.plusSeconds(600), now);
        account.completeVerification("token", now.plusSeconds(60));
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.of(account));
    }

    @Test
    void createsChallengeWithDefaults() {
        when(challenges.saveAndFlush(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChallengeDto dto = service.create("100", "123456789012345678", "Daily Sprint",
                null, LocalDate.of(2026, 12, 31), null);

        assertThat(dto.getName()).isEqualTo("Daily Sprint");
        assertThat(dto.getDiscordGuildId()).isEqualTo("100");
        assertThat(dto.getCreatorDiscordUserId()).isEqualTo("123456789012345678");
        assertThat(dto.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(dto.getTimezone()).isEqualTo("Africa/Casablanca");
        verify(challenges).saveAndFlush(any(Challenge.class));
    }

    @Test
    void rejectsInvalidTimezone() {
        assertThatThrownBy(() -> service.create("100", "123456789012345678", "Sprint",
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 12, 31), "Invalid/Zone"))
                .isInstanceOf(ChallengeCreationException.class)
                .extracting("reason")
                .isEqualTo(INVALID_TIMEZONE);
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> service.create("100", "123456789012345678", "Sprint",
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 24), "UTC"))
                .isInstanceOf(ChallengeCreationException.class)
                .extracting("reason")
                .isEqualTo(END_DATE_BEFORE_START);
    }

    @Test
    void rejectsStartDateInThePast() {
        assertThatThrownBy(() -> service.create("100", "123456789012345678", "Sprint",
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 31), "UTC"))
                .isInstanceOf(ChallengeCreationException.class)
                .extracting("reason")
                .isEqualTo(START_DATE_IN_PAST);
    }

    @Test
    void rejectsUnregisteredCreator() {
        when(users.findByDiscordUserId("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("100", "999", "Sprint",
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 31), "UTC"))
                .isInstanceOf(ChallengeCreationException.class)
                .extracting("reason")
                .isEqualTo(USER_NOT_REGISTERED);
    }

    @Test
    void rejectsUnverifiedCreator() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345678")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("100", "123456789012345678", "Sprint",
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 31), "UTC"))
                .isInstanceOf(ChallengeCreationException.class)
                .extracting("reason")
                .isEqualTo(ACCOUNT_NOT_VERIFIED);
    }
}
