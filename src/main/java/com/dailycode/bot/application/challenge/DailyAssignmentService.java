package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyAssignmentDto;
import com.dailycode.bot.application.problem.ProblemSelectionService;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static com.dailycode.bot.application.challenge.DailyAssignmentException.Reason.*;

@Service
@Validated
@RequiredArgsConstructor
public class DailyAssignmentService {

    private final ChallengeRepository challenges;
    private final DailyChallengeRepository assignments;
    private final ProblemSelectionService selection;
    private final Clock clock;

    @Transactional
    public DailyAssignmentDto assignToday(@NotNull @Positive Long challengeId,
                                           @NotBlank @Size(max = 20) String guildId,
                                           @NotNull ProblemDifficulty difficulty) {
        Challenge challenge = challenges.findForAssignment(challengeId, guildId.strip())
                .orElseThrow(() -> new DailyAssignmentException(CHALLENGE_NOT_FOUND));
        // Resolve today only after acquiring the lock, including a wait across midnight.
        ZoneId zone = ZoneId.of(challenge.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        var existing = assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                challengeId, guildId.strip(), today);
        if (existing.isPresent()) {
            return toDto(existing.get(), false);
        }
        if (challenge.getStatus() == ChallengeStatus.CANCELLED) {
            throw new DailyAssignmentException(CHALLENGE_CANCELLED);
        }
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            throw new DailyAssignmentException(CHALLENGE_COMPLETED);
        }
        if (today.isBefore(challenge.getStartDate())) {
            throw new DailyAssignmentException(CHALLENGE_NOT_STARTED);
        }
        if (today.isAfter(challenge.getEndDate())) {
            throw new DailyAssignmentException(CHALLENGE_ENDED);
        }
        Problem problem = selection.selectForAssignment(challengeId, difficulty)
                .orElseThrow(() -> new DailyAssignmentException(NO_ELIGIBLE_PROBLEM));
        if (!today.equals(LocalDate.now(clock.withZone(zone)))) {
            // A metadata lock may have waited past midnight. Retry the whole operation.
            throw new DailyAssignmentException(DAY_CHANGED);
        }
        DailyChallenge assignment = assignments.saveAndFlush(new DailyChallenge(challenge, problem, today));
        return toDto(assignment, true);
    }

    private DailyAssignmentDto toDto(DailyChallenge assignment, boolean created) {
        Challenge challenge = assignment.getChallenge();
        Problem problem = assignment.getProblem();
        return new DailyAssignmentDto(assignment.getId(), challenge.getId(), problem.getId(),
                assignment.getAssignmentDate(),
                ChronoUnit.DAYS.between(challenge.getStartDate(), assignment.getAssignmentDate()) + 1,
                problem.getTitle(), problem.getTitleSlug(), problem.getDifficulty(),
                assignment.getAssignmentDate().plusDays(1).atStartOfDay(ZoneId.of(challenge.getTimezone())).toInstant(),
                assignment.getAssignedAt(), created);
    }
}
