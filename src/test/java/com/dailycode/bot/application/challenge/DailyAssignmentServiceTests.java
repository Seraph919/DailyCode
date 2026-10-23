package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyAssignmentDto;
import com.dailycode.bot.application.problem.ProblemSelectionService;
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

import static com.dailycode.bot.application.challenge.DailyAssignmentException.Reason.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DailyAssignmentServiceTests {

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final DailyChallengeRepository assignments = mock(DailyChallengeRepository.class);
    private final ProblemSelectionService selection = mock(ProblemSelectionService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));

    private final DailyAssignmentService service = new DailyAssignmentService(challenges, assignments, selection, clock);

    private final DiscordUser creator = new DiscordUser("123456789012345601", "creator");
    private Challenge challenge;
    private Problem problem;

    @BeforeEach
    void setUp() {
        challenge = new Challenge("100", "Sprint", creator,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "UTC");
        problem = new Problem("1", "1", "Two Sum", "two-sum", ProblemDifficulty.EASY, false);

        when(challenges.findForAssignment(1L, "100")).thenReturn(Optional.of(challenge));
        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                1L, "100", LocalDate.of(2026, 10, 23))).thenReturn(Optional.empty());
        when(selection.selectForAssignment(1L, ProblemDifficulty.EASY)).thenReturn(Optional.of(problem));
        when(assignments.saveAndFlush(any(DailyChallenge.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assignsProblemWhenNoneAssignedToday() {
        DailyAssignmentDto dto = service.assignToday(1L, "100", ProblemDifficulty.EASY);

        assertThat(dto.getTitle()).isEqualTo("Two Sum");
        assertThat(dto.getTitleSlug()).isEqualTo("two-sum");
        assertThat(dto.getAssignmentDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(dto.isCreated()).isTrue();
        verify(assignments).saveAndFlush(any(DailyChallenge.class));
    }

    @Test
    void returnsExistingAssignmentWithoutReassigning() {
        DailyChallenge existing = new DailyChallenge(challenge, problem, LocalDate.of(2026, 10, 23));
        when(assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                1L, "100", LocalDate.of(2026, 10, 23))).thenReturn(Optional.of(existing));

        DailyAssignmentDto dto = service.assignToday(1L, "100", ProblemDifficulty.EASY);

        assertThat(dto.getTitleSlug()).isEqualTo("two-sum");
        assertThat(dto.isCreated()).isFalse();
        verify(selection, never()).selectForAssignment(anyLong(), any());
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test
    void throwsWhenChallengeNotFound() {
        when(challenges.findForAssignment(999L, "100")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToday(999L, "100", ProblemDifficulty.EASY))
                .isInstanceOf(DailyAssignmentException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_NOT_FOUND);
    }

    @Test
    void throwsWhenNoEligibleProblem() {
        when(selection.selectForAssignment(1L, ProblemDifficulty.EASY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToday(1L, "100", ProblemDifficulty.EASY))
                .isInstanceOf(DailyAssignmentException.class)
                .extracting("reason")
                .isEqualTo(NO_ELIGIBLE_PROBLEM);
    }

    @Test
    void throwsWhenChallengeNotStartedOrEnded() {
        Challenge futureChallenge = new Challenge("100", "Future", creator,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "UTC");
        when(challenges.findForAssignment(2L, "100")).thenReturn(Optional.of(futureChallenge));

        assertThatThrownBy(() -> service.assignToday(2L, "100", ProblemDifficulty.EASY))
                .isInstanceOf(DailyAssignmentException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_NOT_STARTED);

        Challenge endedChallenge = new Challenge("100", "Ended", creator,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), "UTC");
        when(challenges.findForAssignment(3L, "100")).thenReturn(Optional.of(endedChallenge));

        assertThatThrownBy(() -> service.assignToday(3L, "100", ProblemDifficulty.EASY))
                .isInstanceOf(DailyAssignmentException.class)
                .extracting("reason")
                .isEqualTo(CHALLENGE_ENDED);
    }
}
