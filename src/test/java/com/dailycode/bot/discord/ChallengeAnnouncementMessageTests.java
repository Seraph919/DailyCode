package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.dto.AnnouncementDto;
import com.dailycode.bot.application.challenge.dto.DailyAssignmentDto;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeAnnouncementMessageTests {
    @ParameterizedTest
    @EnumSource(ProblemDifficulty.class)
    void longTitlesFitDiscordLimitsAndButtonsTargetTheChallenge(ProblemDifficulty difficulty) {
        var assignment = new DailyAssignmentDto(10L, 42L, 20L, LocalDate.of(2026, 10, 23), 1,
                "*".repeat(300), "two-sum", difficulty,
                Instant.parse("2026-10-24T00:00:00Z"), Instant.parse("2026-10-23T00:00:00Z"), true);
        try (var message = ChallengeAnnouncementMessage.create(
                new AnnouncementDto("*".repeat(200), "100", "200", null, assignment, "1", 0))) {
            var embed = message.getEmbeds().getFirst();
            assertThat(embed.getTitle()).hasSizeLessThanOrEqualTo(256);
            assertThat(embed.getAuthor().getName()).hasSizeLessThanOrEqualTo(256);
            assertThat(embed.getColorRaw()).isEqualTo(switch (difficulty) {
                case EASY -> 0x2ECC71;
                case MEDIUM -> 0xF1C40F;
                case HARD -> 0xE74C3C;
            });
            var buttons = message.getComponents().getFirst().asActionRow().getButtons();
            assertThat(buttons).hasSize(3);
            assertThat(buttons.get(0).getCustomId()).isEqualTo("dailycode:challenge:join:42");
            assertThat(buttons.get(0).getStyle()).isEqualTo(ButtonStyle.PRIMARY);
            assertThat(buttons.get(1).getCustomId()).isEqualTo("dailycode:challenge:status:42");
            assertThat(buttons.get(2).getStyle()).isEqualTo(ButtonStyle.LINK);
            assertThat(buttons.get(2).getUrl()).isEqualTo("https://leetcode.com/problems/two-sum/");
        }
    }
}
