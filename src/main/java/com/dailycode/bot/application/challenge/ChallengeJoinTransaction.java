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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.dailycode.bot.application.challenge.ChallengeJoinException.Reason.*;

@Component
@RequiredArgsConstructor
public class ChallengeJoinTransaction {

    private final ChallengeRepository challenges;
    private final ChallengeParticipantRepository participants;
    private final DiscordUserRepository users;
    private final LeetCodeAccountRepository accounts;
    private final Clock clock;

    @Transactional
    public ChallengeParticipantDto join(Long challengeId, String guildId, String discordUserId) {
        Challenge challenge = challenges.findForJoin(challengeId, guildId)
                .orElseThrow(() -> new ChallengeJoinException(CHALLENGE_NOT_FOUND));
        if (participants.findMembership(challengeId, guildId, discordUserId).isPresent()) {
            throw new ChallengeJoinException(ALREADY_JOINED);
        }
        if (challenge.getStatus() == ChallengeStatus.CANCELLED) {
            throw new ChallengeJoinException(CHALLENGE_CANCELLED);
        }
        if (challenge.getStatus() == ChallengeStatus.COMPLETED) {
            throw new ChallengeJoinException(CHALLENGE_COMPLETED);
        }
        // Evaluate the inclusive end date after acquiring the challenge lock.
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(challenge.getTimezone())));
        if (today.isAfter(challenge.getEndDate())) {
            throw new ChallengeJoinException(CHALLENGE_ENDED);
        }

        DiscordUser user = users.findByDiscordUserId(discordUserId)
                .orElseThrow(() -> new ChallengeJoinException(USER_NOT_REGISTERED));
        boolean verified = accounts.findByDiscordUser_DiscordUserId(discordUserId)
                .map(LeetCodeAccount::isVerified)
                .orElse(false);
        if (!verified) {
            throw new ChallengeJoinException(ACCOUNT_NOT_VERIFIED);
        }

        ChallengeParticipant participant = new ChallengeParticipant(challenge, user);
        // The unique constraint also protects requests that passed the lookup concurrently.
        participants.saveAndFlush(participant);
        return new ChallengeParticipantDto(participant.getId(), challenge.getId(), challenge.getName(),
                challenge.getDiscordGuildId(), user.getDiscordUserId(), participant.getJoinedAt());
    }
}
