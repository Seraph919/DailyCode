package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.AutomaticCheckTargets;
import com.dailycode.bot.application.challenge.DailyCompletionOutcome;
import com.dailycode.bot.application.challenge.DailyCompletionService;
import com.dailycode.bot.application.challenge.dto.AutomaticCheckBatchDto;
import com.dailycode.bot.application.challenge.dto.AutomaticCheckTargetDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class SubmissionCheckSchedulerTests {

    private final JDA jda = mock(JDA.class);
    private final AutomaticCheckTargets targets = mock(AutomaticCheckTargets.class);
    private final DailyCompletionService completion = mock(DailyCompletionService.class);
    private final SubmissionCheckScheduler scheduler = new SubmissionCheckScheduler(jda, targets, completion);

    private final LocalDate today = LocalDate.of(2026, 10, 23);

    @BeforeEach
    void setUp() {
        when(jda.getStatus()).thenReturn(JDA.Status.CONNECTED);
    }

    @Test
    void doesNothingWhenJdaNotConnected() {
        when(jda.getStatus()).thenReturn(JDA.Status.INITIALIZING);

        scheduler.tick();

        verifyNoInteractions(targets);
        verifyNoInteractions(completion);
    }

    @Test
    void processesTargetsSuccessfully() {
        var target = new AutomaticCheckTargetDto(1L, "100", "123", today);
        var batch1 = new AutomaticCheckBatchDto(5L, List.of(target));
        var batchEnd = new AutomaticCheckBatchDto(null, List.of());
        when(targets.next(0L)).thenReturn(batch1);
        when(targets.next(5L)).thenReturn(batchEnd);

        var result = new DailyCompletionResultDto(1L, 100L, today, 1000L, DailyCompletionOutcome.COMPLETED, "sub1", Instant.now());
        when(completion.checkAutomatic(1L, "100", "123", today)).thenReturn(Optional.of(result));

        scheduler.tick();

        verify(targets).next(0L);
        verify(completion).checkAutomatic(1L, "100", "123", today);
        verify(targets).next(5L);
    }

    @Test
    void pausesScanWhenLeetCodeThrowsException() {
        var target1 = new AutomaticCheckTargetDto(1L, "100", "123", today);
        var target2 = new AutomaticCheckTargetDto(1L, "100", "124", today);
        var batch = new AutomaticCheckBatchDto(11L, List.of(target1, target2));
        when(targets.next(0L)).thenReturn(batch);

        when(completion.checkAutomatic(1L, "100", "123", today))
                .thenThrow(new LeetCodeClientException("Rate limited"));

        scheduler.tick();

        // After target1 throws LeetCodeClientException, target2 must not be checked in this tick (checks paused)
        verify(completion).checkAutomatic(1L, "100", "123", today);
        verify(completion, never()).checkAutomatic(1L, "100", "124", today);
    }
}
