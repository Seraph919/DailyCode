package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.dto.AnnouncementDto;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.List;
import java.util.Locale;

final class ChallengeAnnouncementMessage {
    private ChallengeAnnouncementMessage() {}

    static MessageCreateData create(AnnouncementDto value) {
        var assignment = value.getAssignment();
        String url = "https://leetcode.com/problems/" + assignment.getTitleSlug() + "/";
        String difficulty = assignment.getDifficulty().name().toLowerCase(Locale.ROOT);
        difficulty = Character.toUpperCase(difficulty.charAt(0)) + difficulty.substring(1);
        int color = switch (assignment.getDifficulty()) {
            case EASY -> 0x2ECC71;
            case MEDIUM -> 0xF1C40F;
            case HARD -> 0xE74C3C;
        };
        String title = "#" + value.getFrontendId() + ". " + assignment.getTitle();
        long deadline = assignment.getDeadline().getEpochSecond();
        var embed = new EmbedBuilder()
                .setColor(color)
                .setAuthor(bounded("Challenge #" + assignment.getChallengeId() + " · " + value.getChallengeName(), 256))
                .setTitle(bounded(title, 256), url)
                .setDescription("Solve today's problem on LeetCode before the deadline. Join the challenge to track your progress.")
                .addField("Difficulty", difficulty, true)
                .addField("Participants", value.getParticipantCount() + " coders", true)
                .addField("Verification", "Automatic LeetCode checks", true)
                .addField("Goal", "1 accepted submission", true)
                .addField("Deadline", "<t:" + deadline + ":F>\n<t:" + deadline + ":R>", true)
                .setFooter("Day " + assignment.getDayNumber() + " · " + assignment.getAssignmentDate()
                        + " · Participants counted at posting")
                .build();
        String content = "🚀 **Today's challenge is live!** Sharpen your skills and keep practicing.";
        var message = new MessageCreateBuilder().setAllowedMentions(List.of());
        if (value.getRoleId() != null) {
            content = "<@&" + value.getRoleId() + ">\n" + content;
            message.mentionRoles(value.getRoleId());
        }
        return message.setContent(content).setEmbeds(embed)
                .setComponents(ActionRow.of(
                        Button.primary(ChallengeButtonListener.id("join", assignment.getChallengeId()), "Join Challenge"),
                        Button.secondary(ChallengeButtonListener.id("status", assignment.getChallengeId()), "View Status"),
                        Button.link(url, "LeetCode Problem")))
                .build();
    }

    private static String bounded(String text, int limit) {
        String escaped = MarkdownSanitizer.escape(text);
        return escaped.length() <= limit ? escaped : escaped.substring(0, limit - 3) + "...";
    }
}
