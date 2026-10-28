package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.dto.ChallengeProgressDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChallengeStatusCardTests {
    @Test
    void statusHasDistinctFieldsForCountsScheduleAndEachHistoryDay() {
        var date = LocalDate.of(2026, 10, 28);
        var history = IntStream.range(0, 5).mapToObj(i -> new ChallengeProgressDto.Day(
                date.minusDays(i), "*".repeat(300), i == 0 ? "Completed" : "Completed (late)")).toList();
        var dto = new ChallengeProgressDto(1L, "*".repeat(100), "Africa/Casablanca", date,
                "Completed", true, LocalTime.NOON, 5, 1, 2, history);
        try (var card = ChallengeReplyMessage.status(dto)) {
            assertThat(card.getContent()).isEmpty();
            assertThat(card.getAllowedMentions()).isEmpty();
            assertThat(card.getEmbeds()).hasSize(1);
            var embed = card.getEmbeds().getFirst();
            assertThat(embed.getTitle()).hasSizeLessThanOrEqualTo(256);
            assertThat(embed.getDescription()).contains("page 1/2");
            assertThat(embed.getFields()).hasSize(8).allSatisfy(field -> {
                assertThat(field.getName()).hasSizeLessThanOrEqualTo(256);
                assertThat(field.getValue()).hasSizeLessThanOrEqualTo(1024);
            });
            assertThat(embed.getFields().get(0).getValue()).isEqualTo("5");
            assertThat(embed.getFields().get(2).getValue()).contains("12:00", "Africa/Casablanca");
            assertThat(embed.getFields().get(4).getValue()).contains("Completed (late)");
            assertThat(embed.getFooter().getText()).contains("/challenge check id:1");
            assertThat(CardMatchers.text(card.getEmbeds()).length()).isLessThan(6000);
        }
    }

    @Test
    void emptyStatusCardShowsDisabledAutomationAndNoAssignments() {
        var dto = new ChallengeProgressDto(1L, "Practice", "UTC", LocalDate.of(2026, 10, 28),
                "No problem assigned", false, LocalTime.MIDNIGHT, 0, 1, 1, List.of());
        try (var card = ChallengeReplyMessage.status(dto)) {
            assertThat(CardMatchers.text(card.getEmbeds())).contains("Disabled", "No assignments yet.");
        }
    }
}
