package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.AutomaticCheckTargets;
import com.dailycode.bot.application.challenge.DailyCompletionException;
import com.dailycode.bot.application.challenge.DailyCompletionService;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class SubmissionCheckScheduler {
    private final JDA jda;
    private final AutomaticCheckTargets targets;
    private final DailyCompletionService completion;

    @Scheduled(fixedDelayString = "${discord.submission-check.interval-ms:300000}",
            initialDelayString = "${discord.submission-check.initial-delay-ms:30000}")
    public void tick() {
        if (jda.getStatus() != JDA.Status.CONNECTED) return;
        try {
            long afterId = 0;
            int checked = 0;
            while (true) {
                var batch = targets.next(afterId);
                if (batch.getNextParticipantId() == null) {
                    log.info("Automatic submission scan finished: {} checks.", checked);
                    return;
                }
                for (var target : batch.getTargets()) {
                    if (Thread.currentThread().isInterrupted() || jda.getStatus() != JDA.Status.CONNECTED) return;
                    // One worker, with at least a second between checks; no burst of parallel HTTP calls.
                    if (checked > 0) Thread.sleep(1000);
                    checked++;
                    try {
                        var result = completion.checkAllAutomatic(target.getChallengeId(), target.getGuildId(),
                                target.getDiscordUserId());
                        if (result.newlyCompleted() > 0) {
                            log.info("Automatically recorded {} completions for challenge {} and participant user {}.",
                                    result.newlyCompleted(), target.getChallengeId(), target.getDiscordUserId());
                        }
                    } catch (DailyCompletionException failure) {
                        log.debug("Skipped automatic check for challenge {} ({}).",
                                target.getChallengeId(), failure.getReason());
                    } catch (LeetCodeClientException failure) {
                        // Back off for the remainder of this scan on upstream failures, including rate limits.
                        log.warn("Submission checks paused until the next scan after a LeetCode error.");
                        return;
                    } catch (RuntimeException failure) {
                        log.warn("Automatic check failed for challenge {} ({}).",
                                target.getChallengeId(), failure.getClass().getSimpleName());
                    }
                }
                afterId = batch.getNextParticipantId();
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException failure) {
            log.warn("Submission check scan failed ({}).", failure.getClass().getSimpleName());
        }
    }
}
