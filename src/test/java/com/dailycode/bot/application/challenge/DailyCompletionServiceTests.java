package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyCompletionServiceTests {

    private final DailyCompletionTransaction transaction = mock(DailyCompletionTransaction.class);
    private final LeetCodeClient client = mock(LeetCodeClient.class);
    private final DailyCompletionService service = new DailyCompletionService(transaction, client);

    private final LocalDate today = LocalDate.of(2026, 10, 23);
    private final Instant eligibleFrom = Instant.parse("2026-10-23T00:00:00Z");
    private final Instant deadline = Instant.parse("2026-10-24T00:00:00Z");

    @Test
    void checkTodayFetchesSubmissionsAndCompletes() {
        var check = new DailyCompletionCheckDto(
                1L, "100", "123", 10L, 100L, today, 1000L, "two-sum",
                50L, "alice_lc", eligibleFrom.minusSeconds(86400), eligibleFrom, deadline, null);
        when(transaction.prepare(1L, "100", "123", null)).thenReturn(check);

        var feed = new LeetCodeRecentSubmissionsDto("alice_lc", List.of());
        when(client.fetchRecentAcceptedSubmissions("alice_lc")).thenReturn(feed);

        var expectedResult = new DailyCompletionResultDto(1L, 100L, today, 1000L, DailyCompletionOutcome.COMPLETED, "999", Instant.now());
        when(transaction.complete(check, feed)).thenReturn(expectedResult);

        DailyCompletionResultDto result = service.checkToday(1L, "100", "123");

        assertThat(result.getOutcome()).isEqualTo(DailyCompletionOutcome.COMPLETED);
        verify(transaction).prepare(1L, "100", "123", null);
        verify(client).fetchRecentAcceptedSubmissions("alice_lc");
        verify(transaction).complete(check, feed);
    }

    @Test
    void returnsExistingCompletionWithoutCallingLeetCode() {
        var existingResult = new DailyCompletionResultDto(1L, 100L, today, 1000L, DailyCompletionOutcome.ALREADY_COMPLETED, "999", Instant.now());
        var checkWithExisting = new DailyCompletionCheckDto(
                1L, "100", "123", 10L, 100L, today, 1000L, "two-sum",
                50L, "alice_lc", eligibleFrom.minusSeconds(86400), eligibleFrom, deadline, existingResult);

        when(transaction.prepare(1L, "100", "123", today)).thenReturn(checkWithExisting);

        DailyCompletionResultDto result = service.check(1L, "100", "123", today);

        assertThat(result).isSameAs(existingResult);
        verifyNoInteractions(client);
        verify(transaction, never()).complete(any(), any());
    }

    @Test
    void checkAutomaticReturnsEmptyWhenNothingPrepared() {
        when(transaction.prepareAutomatic(1L, "100", "123", today)).thenReturn(Optional.empty());

        Optional<DailyCompletionResultDto> result = service.checkAutomatic(1L, "100", "123", today);

        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void checkAutomaticFetchesSubmissionsAndCompletes() {
        var check = new DailyCompletionCheckDto(
                1L, "100", "123", 10L, 100L, today, 1000L, "two-sum",
                50L, "alice_lc", eligibleFrom.minusSeconds(86400), eligibleFrom, deadline, null);
        when(transaction.prepareAutomatic(1L, "100", "123", today)).thenReturn(Optional.of(check));

        var feed = new LeetCodeRecentSubmissionsDto("alice_lc", List.of());
        when(client.fetchRecentAcceptedSubmissions("alice_lc")).thenReturn(feed);

        var expectedResult = new DailyCompletionResultDto(1L, 100L, today, 1000L, DailyCompletionOutcome.COMPLETED, "999", Instant.now());
        when(transaction.completeAutomatic(check, feed)).thenReturn(Optional.of(expectedResult));

        Optional<DailyCompletionResultDto> result = service.checkAutomatic(1L, "100", "123", today);

        assertThat(result).contains(expectedResult);
        verify(client).fetchRecentAcceptedSubmissions("alice_lc");
        verify(transaction).completeAutomatic(check, feed);
    }
}
