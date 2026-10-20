package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
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

@Service
@Validated
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NEVER)
public class DailyCompletionService {
    private final DailyCompletionTransaction transaction;
    private final LeetCodeClient client;

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
