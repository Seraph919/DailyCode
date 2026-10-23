package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeTodayDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static com.dailycode.bot.application.challenge.ChallengeTodayException.Reason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChallengeTodayServiceTests {

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final DailyChallengeRepository assignments = mock(DailyChallengeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));

    private final ChallengeTodayService service = new ChallengeTodayService(challenges, assignments, clock);

    private final DiscordUser creator = new DiscordUser("123456789012345601", "creator");
    private Challenge challenge;
    private Problem problem;

    @BeforeEach
    void setUp() {
        challenge = new Challenge("100", "Sprint", creator,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "UTC");
        problem = new Problem("1", "1", "Two Sum", "two-sum", ProblemDifficulty.EASY, false);
        when(challenges.findByIdAndDiscordGuildId(1L, "100")).thenReturn(Optional.of(challenge));
    }

    @Test
    void returnsTodayAssignment() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        DailyChallenge daily = new DailyChallenge(challenge, problem, today);
        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(1L, "100", today))
                .thenReturn(Optional.of(daily));

        ChallengeTodayDto dto = service.today(1L, "100");

        assertThat(dto.getChallengeName()).isEqualTo("Sprint");
        assertThat(dto.getTitle()).isEqualTo("Two Sum");
        assertThat(dto.getTitleSlug()).isEqualTo("two-sum");
        assertThat(dto.getDate()).isEqualTo(today);
        assertThat(dto.getDayNumber()).isEqualTo(23);
    }

    @Test
    void throwsWhenNoAssignmentForToday() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(1L, "100", today))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.today(1L, "100"))
                .isInstanceOf(ChallengeTodayException.class)
                .extracting("reason")
                .isEqualTo(NO_ASSIGNMENT);
    }

    @Test
    void throwsWhenChallengeNotFound() {
        when(challenges.findByIdAndDiscordGuildId(999L, "100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.today(999L, "100"))
                .isInstanceOf(ChallengeTodayException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_NOT_FOUND);
    }

    @Test
    void throwsWhenChallengeCancelledOrEnded() {
        Challenge pastChallenge = new Challenge("100", "Past", creator,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), "UTC");
        when(challenges.findByIdAndDiscordGuildId(2L, "100")).thenReturn(Optional.of(pastChallenge));

        assertThatThrownBy(() -> service.today(2L, "100"))
                .isInstanceOf(ChallengeTodayException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_ENDED);
    }
}

