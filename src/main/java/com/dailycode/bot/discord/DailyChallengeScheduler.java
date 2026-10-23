package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.application.challenge.DailyAssignmentException;
import com.dailycode.bot.application.challenge.dto.AnnouncementDto;
import com.dailycode.bot.application.challenge.dto.AutomationTargetDto;
import com.dailycode.bot.application.problem.ProblemCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class DailyChallengeScheduler {
    private final JDA jda;
    private final ChallengeAutomationService automation;
    private final Clock clock;
    private final ProblemCatalogService catalog;

    @Scheduled(fixedDelayString = "${discord.automation.interval-ms:60000}", initialDelayString = "${discord.automation.initial-delay-ms:15000}")
    public void tick() {
        if (jda.getStatus() != JDA.Status.CONNECTED) return;
        try {
            long afterId = 0;
            while (true) {
                var targets = automation.targets(afterId);
                if (targets.isEmpty()) return;
                for (var target : targets) {
                    if (Thread.currentThread().isInterrupted()) return;
                    process(target);
                    afterId = target.getChallengeId();
                }
            }
        } catch (RuntimeException failure) {
            log.warn("Daily automation scan failed ({}).", failure.getClass().getSimpleName());
        }
    }

    private void process(AutomationTargetDto target) {
        try {
            var guild = jda.getGuildById(target.getGuildId());
            TextChannel channel = guild == null ? null : guild.getTextChannelById(target.getChannelId());
            // No claim is consumed when the bot is offline or cannot access the channel.
            if (channel == null || !channel.canTalk()
                    || !guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)) {
                log.warn("Daily automation cannot access channel for challenge {}.", target.getChallengeId());
                return;
            }
            var prepared = prepareWithCatalog(target);
            if (prepared.isEmpty()) return;
            AnnouncementDto announcement = prepared.get();
            if (!clock.instant().isBefore(announcement.getAssignment().getDeadline())) {
                log.warn("Announcement {} expired before delivery; inspect before retrying.", announcement.getAssignment().getId());
                return;
            }
            // The durable attempt is committed before network I/O. Never blindly resend
            // after a timeout/crash: Discord may already have accepted the message.
            var sent = channel.sendMessage(ChallengeAnnouncementMessage.create(announcement))
                    .setNonce("daily-" + announcement.getAssignment().getId())
                    .timeout(20, TimeUnit.SECONDS).submit().get(25, TimeUnit.SECONDS);
            automation.delivered(announcement.getAssignment().getId(), sent.getId());
            log.info("Announced assignment {} in channel {}.", announcement.getAssignment().getId(), target.getChannelId());
        } catch (DailyAssignmentException failure) {
            if (failure.getReason() == DailyAssignmentException.Reason.NO_ELIGIBLE_PROBLEM) {
                log.warn("Challenge {} is waiting for an unused problem; automatic catalog fetching will retry.", target.getChallengeId());
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            log.error("Daily automation failed for challenge {} ({}); inspect announcement state before retrying.",
                    target.getChallengeId(), failure.getClass().getSimpleName());
        }
    }

    private Optional<AnnouncementDto> prepareWithCatalog(AutomationTargetDto target) {
        try {
            return automation.prepare(target.getChallengeId(), target.getGuildId(), target.getChannelId());
        } catch (DailyAssignmentException failure) {
            if (failure.getReason() != DailyAssignmentException.Reason.NO_ELIGIBLE_PROBLEM) throw failure;
        }
        // The failed assignment transaction has rolled back before any network request.
        catalog.replenish(target.getDifficulty());
        // Recheck the current schedule, channel, lifecycle, difficulty and duplicate claim
        // in a new transaction: an admin may have changed settings during the fetch.
        return automation.prepare(target.getChallengeId(), target.getGuildId(), target.getChannelId());
    }

}
