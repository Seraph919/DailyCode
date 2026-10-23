package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeTodayDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.problem.Problem;
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

import static com.dailycode.bot.application.challenge.ChallengeTodayException.Reason.*;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeTodayService {

    private final ChallengeRepository challenges;
    private final DailyChallengeRepository assignments;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ChallengeTodayDto today(@NotNull @Positive Long challengeId,
                                   @NotBlank @Size(max = 20) String guildId) {
        Challenge challenge = challenges.findByIdAndDiscordGuildId(challengeId, guildId.strip())
                .orElseThrow(() -> new ChallengeTodayException(CHALLENGE_NOT_FOUND));
        if (challenge.getStatus() == ChallengeStatus.CANCELLED) {
            throw new ChallengeTodayException(CHALLENGE_CANCELLED);
        }
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            throw new ChallengeTodayException(CHALLENGE_COMPLETED);
        }
        ZoneId zone = ZoneId.of(challenge.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (today.isBefore(challenge.getStartDate())) {
            throw new ChallengeTodayException(CHALLENGE_NOT_STARTED);
        }
        if (today.isAfter(challenge.getEndDate())) {
            throw new ChallengeTodayException(CHALLENGE_ENDED);
        }
        DailyChallenge assignment = assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                        challengeId, guildId.strip(), today)
                .orElseThrow(() -> {
                    var postingAt = challenge.getAnnouncementChannelId() == null ? null
                            : today.atTime(challenge.getAutomationTime()).atZone(zone).toInstant();
                    return new ChallengeTodayException(NO_ASSIGNMENT, new ChallengeTodayException.Schedule(
                            today, zone.getId(), postingAt,
                            postingAt != null && !clock.instant().isBefore(postingAt)));
                });
        Problem problem = assignment.getProblem();
        return new ChallengeTodayDto(challengeId, challenge.getName(),
                ChronoUnit.DAYS.between(challenge.getStartDate(), today) + 1, today, zone.getId(),
                problem.getTitle(), problem.getTitleSlug(), problem.getDifficulty(),
                today.plusDays(1).atStartOfDay(zone).toInstant());
    }
}
