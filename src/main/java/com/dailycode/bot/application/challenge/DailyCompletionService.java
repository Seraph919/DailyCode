package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import com.dailycode.bot.application.challenge.dto.ChallengeCompletionResultDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Objects;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NEVER)
public class DailyCompletionService {
    private final DailyCompletionTransaction transaction;
    private final LeetCodeClient client;

    public ChallengeCompletionResultDto checkAll(@NotNull @Positive Long challengeId,
                                                 @NotBlank @Size(max = 20) String guildId,
                                                 @NotBlank @Size(max = 20) String discordUserId) {
        return checkAllInternal(challengeId, guildId.strip(), discordUserId.strip(), false);
    }

    public ChallengeCompletionResultDto checkAllAutomatic(@NotNull @Positive Long challengeId,
                                                          @NotBlank @Size(max = 20) String guildId,
                                                          @NotBlank @Size(max = 20) String discordUserId) {
        return checkAllInternal(challengeId, guildId.strip(), discordUserId.strip(), true);
    }

    private ChallengeCompletionResultDto checkAllInternal(Long challengeId, String guild, String user, boolean automatic) {
        var dates = transaction.assignmentDates(challengeId, guild, user, automatic);
        LeetCodeRecentSubmissionsDto feed = null;
        DailyCompletionCheckDto feedTarget = null;
        int completed = 0, existing = 0, noMatch = 0;
        for (LocalDate date : dates) {
            var prepared = automatic ? transaction.prepareAutomatic(challengeId, guild, user, date)
                    : Optional.of(transaction.prepare(challengeId, guild, user, date));
            if (prepared.isEmpty()) break; // Automation was disabled during this scan.
            var check = prepared.get();
            DailyCompletionResultDto result = check.getExistingCompletion();
            if (result == null) {
                if (feed == null) {
                    feed = client.fetchRecentAcceptedSubmissions(check.getUsername());
                    feedTarget = check;
                } else if (!Objects.equals(feedTarget.getAccountId(), check.getAccountId())
                        || !feedTarget.getUsername().equalsIgnoreCase(check.getUsername())
                        || !Objects.equals(feedTarget.getAccountVerifiedAt(), check.getAccountVerifiedAt())) {
                    throw new DailyCompletionException(DailyCompletionException.Reason.STATE_CHANGED);
                }
                // Each write revalidates membership/account and commits independently;
                // no database lock spans the single upstream request for this challenge.
                var saved = automatic ? transaction.completeAutomatic(check, feed)
                        : Optional.of(transaction.complete(check, feed));
                if (saved.isEmpty()) break;
                result = saved.get();
            }
            switch (result.getOutcome()) {
                case COMPLETED -> completed++;
                case ALREADY_COMPLETED -> existing++;
                case NO_MATCH -> noMatch++;
            }
        }
        return new ChallengeCompletionResultDto(challengeId, completed + existing + noMatch, completed, existing, noMatch);
    }

    public Optional<DailyCompletionResultDto> checkAutomatic(@NotNull @Positive Long challengeId,
                                                            @NotBlank @Size(max = 20) String guildId,
                                                            @NotBlank @Size(max = 20) String discordUserId,
                                                            @NotNull LocalDate date) {
        var prepared = transaction.prepareAutomatic(challengeId, guildId.strip(), discordUserId.strip(), date);
        if (prepared.isEmpty()) return Optional.empty();
        var check = prepared.get();
        if (check.getExistingCompletion() != null) return Optional.of(check.getExistingCompletion());
        return transaction.completeAutomatic(check, client.fetchRecentAcceptedSubmissions(check.getUsername()));
    }

    public DailyCompletionResultDto checkToday(@NotNull @Positive Long challengeId,
                                               @NotBlank @Size(max = 20) String guildId,
                                               @NotBlank @Size(max = 20) String discordUserId) {
        return fetchAndComplete(transaction.prepare(challengeId, guildId.strip(), discordUserId.strip(), null));
    }

    public DailyCompletionResultDto check(@NotNull @Positive Long challengeId,
                                          @NotBlank @Size(max = 20) String guildId,
                                          @NotBlank @Size(max = 20) String discordUserId,
                                          @NotNull LocalDate assignmentDate) {
        return fetchAndComplete(transaction.prepare(challengeId, guildId.strip(), discordUserId.strip(), assignmentDate));
    }

    private DailyCompletionResultDto fetchAndComplete(DailyCompletionCheckDto check) {
        if (check.getExistingCompletion() != null) {
            return check.getExistingCompletion();
        }
        var submissions = client.fetchRecentAcceptedSubmissions(check.getUsername());
        return transaction.complete(check, submissions);
    }
}
