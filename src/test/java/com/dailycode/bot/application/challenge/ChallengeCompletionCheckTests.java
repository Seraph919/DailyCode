package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.*;
import com.dailycode.bot.application.leetcode.dto.*;
import com.dailycode.bot.leetcode.LeetCodeClient;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChallengeCompletionCheckTests {
    private final DailyCompletionTransaction transaction = mock(DailyCompletionTransaction.class);
    private final LeetCodeClient client = mock(LeetCodeClient.class);
    private final DailyCompletionService service = new DailyCompletionService(transaction, client);
    private final LocalDate firstDay = LocalDate.of(2026, 10, 1);

    private DailyCompletionCheckDto check(LocalDate date, String username, DailyCompletionResultDto existing) {
        return new DailyCompletionCheckDto(1L, "2", "3", 4L, (long) date.getDayOfMonth(), date,
                5L, "two-sum", 6L, username, Instant.parse("2026-09-01T00:00:00Z"),
                date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC), existing);
    }

    private DailyCompletionResultDto result(LocalDate date, DailyCompletionOutcome outcome) {
        return new DailyCompletionResultDto(1L, (long) date.getDayOfMonth(), date, 7L, outcome, null, null);
    }

    @Test
    void checksEntireHistoryWithOneFeedRequestAndKeepsExistingCompletions() {
        var second = firstDay.plusDays(10);
        var third = firstDay.plusDays(20);
        when(transaction.assignmentDates(1L, "2", "3", false)).thenReturn(List.of(firstDay, second, third));
        var old = check(firstDay, "alice", result(firstDay, DailyCompletionOutcome.ALREADY_COMPLETED));
        var late = check(second, "alice", null);
        var unmatched = check(third, "alice", null);
        when(transaction.prepare(1L, "2", "3", firstDay)).thenReturn(old);
        when(transaction.prepare(1L, "2", "3", second)).thenReturn(late);
        when(transaction.prepare(1L, "2", "3", third)).thenReturn(unmatched);
        var feed = new LeetCodeRecentSubmissionsDto("alice", List.of());
        when(client.fetchRecentAcceptedSubmissions("alice")).thenReturn(feed);
        when(transaction.complete(late, feed)).thenReturn(result(second, DailyCompletionOutcome.COMPLETED));
        when(transaction.complete(unmatched, feed)).thenReturn(result(third, DailyCompletionOutcome.NO_MATCH));

        assertThat(service.checkAll(1L, " 2 ", " 3 "))
                .isEqualTo(new ChallengeCompletionResultDto(1L, 3, 1, 1, 1));
        verify(client, times(1)).fetchRecentAcceptedSubmissions("alice");
        verify(transaction, never()).complete(eq(old), any());
    }

    @Test
    void completedHistoryNeedsNoLeetCodeRequest() {
        when(transaction.assignmentDates(1L, "2", "3", false)).thenReturn(List.of(firstDay));
        when(transaction.prepare(1L, "2", "3", firstDay)).thenReturn(
                check(firstDay, "alice", result(firstDay, DailyCompletionOutcome.ALREADY_COMPLETED)));
        assertThat(service.checkAll(1L, "2", "3").alreadyCompleted()).isEqualTo(1);
        verifyNoInteractions(client);
    }

    @Test
    void emptyHistoryNeedsNoLeetCodeRequest() {
        when(transaction.assignmentDates(1L, "2", "3", false)).thenReturn(List.of());
        assertThat(service.checkAll(1L, "2", "3").checked()).isZero();
        verifyNoInteractions(client);
    }

    @Test
    void changingTheVerifiedAccountStopsFeedReuse() {
        when(transaction.assignmentDates(1L, "2", "3", false)).thenReturn(List.of(firstDay, firstDay.plusDays(1)));
        var first = check(firstDay, "alice", null);
        when(transaction.prepare(1L, "2", "3", firstDay)).thenReturn(first);
        when(transaction.prepare(1L, "2", "3", firstDay.plusDays(1))).thenReturn(check(firstDay.plusDays(1), "bob", null));
        var feed = new LeetCodeRecentSubmissionsDto("alice", List.of());
        when(client.fetchRecentAcceptedSubmissions("alice")).thenReturn(feed);
        when(transaction.complete(first, feed)).thenReturn(result(firstDay, DailyCompletionOutcome.NO_MATCH));
        assertThatThrownBy(() -> service.checkAll(1L, "2", "3")).isInstanceOf(DailyCompletionException.class)
                .hasMessage("STATE_CHANGED");
        verify(transaction, times(1)).complete(any(), any());
    }

    @Test
    void automaticChecksIncludeOldDaysAndStopIfDisabledDuringFetch() {
        when(transaction.assignmentDates(1L, "2", "3", true)).thenReturn(List.of(firstDay, firstDay.plusDays(1)));
        var first = check(firstDay, "alice", null);
        when(transaction.prepareAutomatic(1L, "2", "3", firstDay)).thenReturn(Optional.of(first));
        var feed = new LeetCodeRecentSubmissionsDto("alice", List.of());
        when(client.fetchRecentAcceptedSubmissions("alice")).thenReturn(feed);
        when(transaction.completeAutomatic(first, feed)).thenReturn(Optional.empty());
        assertThat(service.checkAllAutomatic(1L, "2", "3").checked()).isZero();
        verify(transaction, never()).prepareAutomatic(1L, "2", "3", firstDay.plusDays(1));
    }
}
