package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeLeaderboardDto;
import com.dailycode.bot.repository.ChallengeParticipantRepository;
import com.dailycode.bot.repository.ChallengeRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeLeaderboardService {
    private final ChallengeRepository challenges;
    private final ChallengeParticipantRepository participants;

    @Transactional(readOnly = true)
    public ChallengeLeaderboardDto leaderboard(@NotNull @Positive Long challengeId,
            @NotBlank @Size(max = 20) String guildId, @Min(1) @Max(1000000) int page) {
        String guild = guildId.strip();
        var challenge = challenges.findByIdAndDiscordGuildId(challengeId, guild)
                .orElseThrow(() -> new DailyCompletionException(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        var scores = participants.findLeaderboard(challengeId, guild, PageRequest.of(page - 1, 10));
        var entries = scores.stream().map(score -> new ChallengeLeaderboardDto.Entry(
                score.getRank(), score.getDiscordUserId(), score.getCompleted())).toList();
        return new ChallengeLeaderboardDto(challengeId, challenge.getName(), page,
                Math.max(1, scores.getTotalPages()), scores.getTotalElements(), entries);
    }
}
