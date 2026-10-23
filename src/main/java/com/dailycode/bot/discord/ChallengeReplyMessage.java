package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeTodayException;
import com.dailycode.bot.application.challenge.DailyCompletionOutcome;
import com.dailycode.bot.application.challenge.dto.ChallengeTodayDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.List;
import java.util.Locale;

final class ChallengeReplyMessage {
    private ChallengeReplyMessage() {}

    static MessageEditData today(ChallengeTodayDto value) {
        String url = "https://leetcode.com/problems/" + value.getTitleSlug() + "/";
        String difficulty = value.getDifficulty().name().toLowerCase(Locale.ROOT);
        int color = switch (value.getDifficulty()) {
            case EASY -> 0x2ECC71;
            case MEDIUM -> 0xF1C40F;
            case HARD -> 0xE74C3C;
        };
        long deadline = value.getDeadline().getEpochSecond();
        var embed = new EmbedBuilder().setColor(color)
                .setAuthor("Challenge #" + value.getChallengeId() + " · " + value.getChallengeName())
                .setTitle(bounded(value.getTitle()), url)
                .setDescription("Today's assigned problem. Submit an accepted solution before the deadline.")
                .addField("Difficulty", Character.toUpperCase(difficulty.charAt(0)) + difficulty.substring(1), true)
                .addField("Goal", "1 accepted submission", true)
                .addField("Deadline", "<t:" + deadline + ":F>\n<t:" + deadline + ":R>", false)
                .setFooter("Day " + value.getDayNumber() + " · " + value.getDate() + " · " + value.getTimezone());
        return base(embed).setComponents(ActionRow.of(
                Button.primary(ChallengeButtonListener.id("join", value.getChallengeId()), "Join Challenge"),
                Button.secondary(ChallengeButtonListener.id("status", value.getChallengeId()), "View Status"),
                Button.link(url, "LeetCode Problem"))).build();
    }

    static MessageEditData check(DailyCompletionResultDto value) {
        String title = switch (value.getOutcome()) {
            case COMPLETED -> "Challenge completed!";
            case ALREADY_COMPLETED -> "Already completed";
            case NO_MATCH -> "No accepted submission found";
        };
        boolean completed = value.getOutcome() != DailyCompletionOutcome.NO_MATCH;
        var embed = new EmbedBuilder().setColor(completed ? 0x2ECC71 : 0xF1C40F)
                .setTitle(title)
                .setDescription(completed ? "Your completion is recorded for this challenge day."
                        : "No eligible recent accepted submission was found. If you just submitted, try again shortly.")
                .addField("Challenge", "#" + value.getChallengeId(), true)
                .addField("Assignment date", value.getAssignmentDate().toString(), true)
                .setFooter("Dates follow the challenge's timezone");
        if (completed && value.getCompletedAt() != null) {
            embed.addField("Completed", "<t:" + value.getCompletedAt().getEpochSecond() + ":F>", false);
        }
        return base(embed).setComponents(ActionRow.of(Button.secondary(
                ChallengeButtonListener.id("status", value.getChallengeId()), "View Status"))).build();
    }

    static MessageEditData waiting(long challengeId, ChallengeTodayException.Schedule schedule) {
        String description = "No problem has been assigned for today yet.";
        var embed = new EmbedBuilder().setColor(0xF1C40F).setTitle("Today's problem is not ready")
                .addField("Challenge", "#" + challengeId, true);
        if (schedule != null) {
            embed.addField("Challenge date", schedule.date() + " · " + schedule.timezone(), false);
            description += " A new challenge day starts at midnight in this timezone; yesterday's problem stays on its original day.";
            if (schedule.postingAt() == null) {
                description += "\n\nAutomatic posting is disabled. Ask a server manager to configure `/challenge automate`.";
            } else {
                embed.addField("Today's scheduled post", "<t:" + schedule.postingAt().getEpochSecond() + ":F>", false);
                description += schedule.postingTimePassed()
                        ? "\n\nThe posting time has passed. Allow about a minute for the scheduler. If it remains missing, ask a server manager to check the bot's logs and channel permissions."
                        : "\n\nThe bot assigns today's problem at the scheduled posting time. Check again after that time.";
            }
        }
        return base(embed.setDescription(description)).build();
    }

    static MessageEditData notice(String title, String description) {
        return base(new EmbedBuilder().setColor(0xF1C40F).setTitle(title).setDescription(description)).build();
    }

    private static MessageEditBuilder base(EmbedBuilder embed) {
        return new MessageEditBuilder().setReplace(true).setAllowedMentions(List.of()).setEmbeds(embed.build());
    }

    private static String bounded(String value) {
        String escaped = MarkdownSanitizer.escape(value);
        return escaped.length() <= 256 ? escaped : escaped.substring(0, 253) + "...";
    }
}
