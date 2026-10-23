package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeParticipantDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.ChallengeParticipantRepository;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DiscordUserRepository;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.dailycode.bot.application.challenge.ChallengeJoinException.Reason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChallengeJoinServiceTests {

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final ChallengeParticipantRepository participants = mock(ChallengeParticipantRepository.class);
    private final DiscordUserRepository users = mock(DiscordUserRepository.class);
    private final LeetCodeAccountRepository accounts = mock(LeetCodeAccountRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));

    private final ChallengeJoinTransaction joinTransaction = new ChallengeJoinTransaction(
            challenges, participants, users, accounts, clock);
    private final ChallengeJoinService service = new ChallengeJoinService(joinTransaction);

    private final DiscordUser creator = new DiscordUser("123456789012345601", "creator");
    private final DiscordUser member = new DiscordUser("123456789012345602", "member");
    private Challenge challenge;

    @BeforeEach
    void setUp() {
        challenge = new Challenge("100", "Join Test", creator,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31), "UTC");
        when(challenges.findForJoin(1L, "100")).thenReturn(Optional.of(challenge));
        when(participants.findMembership(1L, "100", "123456789012345602")).thenReturn(Optional.empty());
        when(users.findByDiscordUserId("123456789012345602")).thenReturn(Optional.of(member));

        LeetCodeAccount account = new LeetCodeAccount(member, "member_lc");
        Instant now = Instant.now();
        account.startVerification("token", now.plusSeconds(600), now);
        account.completeVerification("token", now.plusSeconds(60));
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345602")).thenReturn(Optional.of(account));

        when(participants.saveAndFlush(any(ChallengeParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void joinsChallengeSuccessfully() {
        ChallengeParticipantDto result = service.join(1L, "100", "123456789012345602");

        assertThat(result.getChallengeName()).isEqualTo("Join Test");
        assertThat(result.getDiscordGuildId()).isEqualTo("100");
        assertThat(result.getDiscordUserId()).isEqualTo("123456789012345602");
        verify(participants).saveAndFlush(any(ChallengeParticipant.class));
    }

    @Test
    void throwsWhenChallengeNotFound() {
        when(challenges.findForJoin(999L, "100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(999L, "100", "123456789012345602"))
                .isInstanceOf(ChallengeJoinException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_NOT_FOUND);
    }

    @Test
    void throwsWhenAlreadyJoined() {
        when(participants.findMembership(1L, "100", "123456789012345602"))
                .thenReturn(Optional.of(mock(ChallengeParticipant.class)));

        assertThatThrownBy(() -> service.join(1L, "100", "123456789012345602"))
                .isInstanceOf(ChallengeJoinException.class)
                .extracting("reason")
                .isEqualTo(ALREADY_JOINED);
    }

    @Test
    void throwsWhenChallengeEnded() {
        Challenge pastChallenge = new Challenge("100", "Past", creator,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 20), "UTC");
        when(challenges.findForJoin(2L, "100")).thenReturn(Optional.of(pastChallenge));

        assertThatThrownBy(() -> service.join(2L, "100", "123456789012345602"))
                .isInstanceOf(ChallengeJoinException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_ENDED);
    }

    @Test
    void throwsWhenUserUnverified() {
        when(accounts.findByDiscordUser_DiscordUserId("123456789012345602")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.join(1L, "100", "123456789012345602"))
                .isInstanceOf(ChallengeJoinException.class)
                .extracting("reason")
                .isEqualTo(ACCOUNT_NOT_VERIFIED);
    }

    @Test
    void translatesConstraintViolationToAlreadyJoined() {
        ChallengeJoinTransaction mockTx = mock(ChallengeJoinTransaction.class);
        ChallengeJoinService joinService = new ChallengeJoinService(mockTx);

        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException("violation", sqlEx, "uk_challenge_participants_challenge_user");
        DataIntegrityViolationException dive = new DataIntegrityViolationException("data error", cve);

        when(mockTx.join(1L, "100", "123456789012345602")).thenThrow(dive);

        assertThatThrownBy(() -> joinService.join(1L, "100", "123456789012345602"))
                .isInstanceOf(ChallengeJoinException.class)
                .extracting("reason")
                .isEqualTo(ALREADY_JOINED);
    }
}
