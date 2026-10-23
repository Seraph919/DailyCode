package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeProgressDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.challenge.DailyProgress;
import com.dailycode.bot.domain.challenge.DailyProgressStatus;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.ChallengeParticipantRepository;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import com.dailycode.bot.repository.DailyProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.dailycode.bot.application.challenge.DailyCompletionException.Reason.CHALLENGE_NOT_FOUND;
import static com.dailycode.bot.application.challenge.DailyCompletionException.Reason.NOT_JOINED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChallengeProgressServiceTests {

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final ChallengeParticipantRepository participants = mock(ChallengeParticipantRepository.class);
    private final DailyChallengeRepository assignments = mock(DailyChallengeRepository.class);
    private final DailyProgressRepository progress = mock(DailyProgressRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));

    private final ChallengeProgressService service = new ChallengeProgressService(
            challenges, participants, assignments, progress, clock);

    private final DiscordUser creator = new DiscordUser("123456789012345601", "creator");
    private final DiscordUser member = new DiscordUser("123456789012345602", "member");
    private Challenge challenge;
    private ChallengeParticipant participant;
    private Problem problem;

    @BeforeEach
    void setUp() {
        challenge = new Challenge("100", "Sprint", creator,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "UTC");
        participant = mock(ChallengeParticipant.class);
        when(participant.getId()).thenReturn(10L);
        when(participant.getJoinedAt()).thenReturn(Instant.parse("2026-10-01T00:00:00Z"));
        problem = new Problem("1", "1", "Two Sum", "two-sum", ProblemDifficulty.EASY, false);

        when(challenges.findByIdAndDiscordGuildId(1L, "100")).thenReturn(Optional.of(challenge));
        when(participants.findMembership(1L, "100", "123456789012345602")).thenReturn(Optional.of(participant));
    }

    @Test
    void returnsProgressSummary() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        DailyChallenge daily = new DailyChallenge(challenge, problem, today);

        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDateLessThanEqualOrderByAssignmentDateDesc(
                eq(1L), eq("100"), eq(today), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(daily)));

        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(1L, "100", today))
                .thenReturn(Optional.of(daily));

        when(progress.findProgress(daily.getId(), "100", "123456789012345602")).thenReturn(Optional.empty());
        when(progress.countByParticipant_IdAndStatus(participant.getId(), DailyProgressStatus.COMPLETED)).thenReturn(3L);

        ChallengeProgressDto result = service.status(1L, "100", "123456789012345602", 1);

        assertThat(result.name()).isEqualTo("Sprint");
        assertThat(result.completed()).isEqualTo(3L);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.history()).hasSize(1);
    }

    @Test
    void throwsWhenChallengeNotFound() {
        when(challenges.findByIdAndDiscordGuildId(999L, "100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.status(999L, "100", "123456789012345602", 1))
                .isInstanceOf(DailyCompletionException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_NOT_FOUND);
    }

    @Test
    void throwsWhenUserNotJoined() {
        when(participants.findMembership(1L, "100", "non_member")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.status(1L, "100", "non_member", 1))
                .isInstanceOf(DailyCompletionException.class)
                .extracting("reason")
                .isEqualTo(NOT_JOINED);
    }
}
