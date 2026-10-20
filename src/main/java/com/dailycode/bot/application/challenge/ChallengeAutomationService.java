package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.AnnouncementDto;
import com.dailycode.bot.application.challenge.dto.AutomationTargetDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.dailycode.bot.application.challenge.DailyAssignmentException.Reason.*;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeAutomationService {
    private final ChallengeRepository challenges;
    private final DailyChallengeRepository daily;
    private final DailyAssignmentService assignments;
    private final Clock clock;

    @Transactional
    public void enable(@NotNull @Positive Long id, @NotBlank @Size(max = 20) String guildId,
                       @NotNull @Pattern(regexp = "[0-9]{1,20}") String channelId,
                       @NotNull ProblemDifficulty difficulty) {
        Challenge challenge = locked(id, guildId);
        checkOpen(challenge);
        challenge.automate(channelId, difficulty);
    }

    @Transactional
    public void disable(@NotNull @Positive Long id, @NotBlank @Size(max = 20) String guildId) {
        locked(id, guildId).stopAutomation();
    }

    @Transactional(readOnly = true)
    public List<AutomationTargetDto> targets(long afterId) {
        return challenges.findByAnnouncementChannelIdIsNotNullAndIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, 50))
                .stream().map(this::target).toList();
    }

    private AutomationTargetDto target(Challenge challenge) {
        return new AutomationTargetDto(challenge.getId(), challenge.getDiscordGuildId(), challenge.getAnnouncementChannelId());
    }

    @Transactional
    public Optional<AnnouncementDto> prepare(Long id, String guildId, String expectedChannel) {
        Challenge challenge = locked(id, guildId);
        if (challenge.getAnnouncementChannelId() == null || !challenge.getAnnouncementChannelId().equals(expectedChannel)) {
            return Optional.empty();
        }
        checkOpen(challenge);
        var assignment = assignments.assignToday(id, guildId, challenge.getAutomationDifficulty());
        DailyChallenge entity = daily.findById(assignment.getId()).orElseThrow();
        if (entity.getAnnouncementAttemptedAt() != null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(assignment.getDeadline())) {
            throw new DailyAssignmentException(DAY_CHANGED);
        }
        entity.claimAnnouncement(expectedChannel, clock.instant());
        daily.flush();
        return Optional.of(new AnnouncementDto(challenge.getName(), guildId, expectedChannel, assignment));
    }

    @Transactional
    public void delivered(Long assignmentId, String messageId) {
        daily.findById(assignmentId).orElseThrow().recordAnnouncement(messageId);
    }

    private Challenge locked(Long id, String guildId) {
        return challenges.findForAssignment(id, guildId.strip())
                .orElseThrow(() -> new DailyAssignmentException(CHALLENGE_NOT_FOUND));
    }

    private void checkOpen(Challenge challenge) {
        if (challenge.getStatus() == ChallengeStatus.CANCELLED) throw new DailyAssignmentException(CHALLENGE_CANCELLED);
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) throw new DailyAssignmentException(CHALLENGE_COMPLETED);
        if (LocalDate.now(clock.withZone(ZoneId.of(challenge.getTimezone()))).isAfter(challenge.getEndDate())) {
            throw new DailyAssignmentException(CHALLENGE_ENDED);
        }
    }
}
