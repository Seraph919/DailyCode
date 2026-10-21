package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeProgressDto;
import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyProgressStatus;
import com.dailycode.bot.repository.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.dailycode.bot.application.challenge.DailyCompletionException.Reason.*;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeProgressService {
    private final ChallengeRepository challenges;
    private final ChallengeParticipantRepository participants;
    private final DailyChallengeRepository assignments;
    private final DailyProgressRepository progress;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ChallengeProgressDto status(@NotNull @Positive Long challengeId,
            @NotBlank @Size(max = 20) String guildId,
            @NotBlank @Size(max = 20) String userId, @Min(1) @Max(1000000) int page) {
        String guild = guildId.strip();
        String user = userId.strip();
        var challenge = challenges.findByIdAndDiscordGuildId(challengeId, guild)
                .orElseThrow(() -> new DailyCompletionException(CHALLENGE_NOT_FOUND));
        var member = participants.findMembership(challengeId, guild, user)
                .orElseThrow(() -> new DailyCompletionException(NOT_JOINED));
        var zone = ZoneId.of(challenge.getTimezone());
        var today = LocalDate.now(clock.withZone(zone));
        var history = assignments
                .findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDateLessThanEqualOrderByAssignmentDateDesc(
                        challengeId, guild, today, PageRequest.of(page - 1, 5));
        var days = history.stream().map(day -> new ChallengeProgressDto.Day(day.getAssignmentDate(),
                day.getProblem().getTitle(), dayStatus(day, member, guild, user, zone))).toList();
        String todayStatus = assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                challengeId, guild, today).map(day -> dayStatus(day, member, guild, user, zone))
                .orElse(challenge.getStatus() == ChallengeStatus.CANCELLED ? "Challenge cancelled"
                        : challenge.getStatus() == ChallengeStatus.COMPLETED ? "Challenge completed"
                        : today.isBefore(challenge.getStartDate()) ? "Challenge has not started"
                        : today.isAfter(challenge.getEndDate()) ? "Challenge has ended"
                        : "No problem assigned");
        return new ChallengeProgressDto(challengeId, challenge.getName(), zone.getId(), today,
                todayStatus, challenge.getAnnouncementChannelId() != null,
                progress.countByParticipant_IdAndStatus(member.getId(), DailyProgressStatus.COMPLETED),
                page, Math.max(1, history.getTotalPages()), days);
    }

    private String dayStatus(DailyChallenge day, ChallengeParticipant member,
            String guild, String user, ZoneId zone) {
        var saved = progress.findProgress(day.getId(), guild, user);
        if (saved.isPresent() && saved.get().getStatus() == DailyProgressStatus.COMPLETED) {
            return "Completed";
        }
        if (!member.getJoinedAt().isBefore(day.getAssignmentDate().plusDays(1).atStartOfDay(zone).toInstant())) {
            return "Before you joined";
        }
        // The recent-submissions feed cannot prove that an old day was missed.
        return "No completion recorded";
    }
}
