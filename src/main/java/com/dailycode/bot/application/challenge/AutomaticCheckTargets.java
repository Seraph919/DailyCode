package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.AutomaticCheckBatchDto;
import com.dailycode.bot.application.challenge.dto.AutomaticCheckTargetDto;
import com.dailycode.bot.repository.ChallengeParticipantRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AutomaticCheckTargets {
    private final ChallengeParticipantRepository participants;
    private final DailyChallengeRepository assignments;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AutomaticCheckBatchDto next(long afterId) {
        var page = participants.findForAutomaticChecks(afterId, PageRequest.of(0, 50));
        if (page.isEmpty()) return new AutomaticCheckBatchDto(null, List.of());
        var targets = new ArrayList<AutomaticCheckTargetDto>();
        for (var participant : page) {
            var challenge = participant.getChallenge();
            ZoneId zone = ZoneId.of(challenge.getTimezone());
            LocalDate today = LocalDate.now(clock.withZone(zone));
            String userId = participant.getDiscordUser().getDiscordUserId();
            if (assignments.hasUncompletedAssignments(challenge.getId(), participant.getId(), today)) {
                targets.add(new AutomaticCheckTargetDto(challenge.getId(), challenge.getDiscordGuildId(), userId));
            }
        }
        return new AutomaticCheckBatchDto(page.getLast().getId(), List.copyOf(targets));
    }
}
