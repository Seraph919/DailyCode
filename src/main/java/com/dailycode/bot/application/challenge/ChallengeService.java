package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.ChallengeDto;
import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DiscordUserRepository;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.dailycode.bot.application.challenge.ChallengeCreationException.Reason.*;

@Service
@Validated
@RequiredArgsConstructor
public class ChallengeService {

    private final DiscordUserRepository users;
    private final LeetCodeAccountRepository accounts;
    private final ChallengeRepository challenges;
    private final Clock clock;

    @Transactional
    public ChallengeDto create(
            @NotBlank @Size(max = 20) String discordGuildId,
            @NotBlank @Size(max = 20) String creatorDiscordUserId,
            @NotBlank @Size(max = 100) String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank @Size(max = 100) String timezone) {
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone.strip());
        } catch (DateTimeException failure) {
            throw new ChallengeCreationException(INVALID_TIMEZONE);
        }
        if (endDate.isBefore(startDate)) {
            throw new ChallengeCreationException(END_DATE_BEFORE_START);
        }
        // "Today" belongs to the challenge's timezone, not the server's timezone.
        if (startDate.isBefore(LocalDate.now(clock.withZone(zone)))) {
            throw new ChallengeCreationException(START_DATE_IN_PAST);
        }

        String creatorId = creatorDiscordUserId.strip();
        DiscordUser creator = users.findByDiscordUserId(creatorId)
                .orElseThrow(() -> new ChallengeCreationException(USER_NOT_REGISTERED));
        boolean verified = accounts.findByDiscordUser_DiscordUserId(creatorId)
                .map(LeetCodeAccount::isVerified)
                .orElse(false);
        if (!verified) {
            throw new ChallengeCreationException(ACCOUNT_NOT_VERIFIED);
        }

        Challenge challenge = new Challenge(discordGuildId, name, creator, startDate, endDate, zone.getId());
        challenges.saveAndFlush(challenge);

        // Return plain data while the transaction still owns the managed entities.
        return new ChallengeDto(challenge.getId(), challenge.getDiscordGuildId(), challenge.getName(),
                creator.getDiscordUserId(), challenge.getStartDate(), challenge.getEndDate(),
                challenge.getTimezone(), challenge.getStatus(), challenge.getCreatedAt());
    }
}
