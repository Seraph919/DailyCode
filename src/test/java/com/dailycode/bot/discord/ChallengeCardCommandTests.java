package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.*;
import com.dailycode.bot.application.challenge.dto.ChallengeTodayDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChallengeCardCommandTests {
    private final ChallengeTodayService today = mock(ChallengeTodayService.class);
    private final DailyCompletionService completion = mock(DailyCompletionService.class);
    private final ChallengeCommandHandler handler = new ChallengeCommandHandler(
            null, null, null, today, null, completion, null, null, new SyncTaskExecutor());
    private final SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    private final InteractionHook hook = mock(InteractionHook.class);
    @SuppressWarnings("unchecked")
    private final WebhookMessageEditAction<Message> edit = mock(WebhookMessageEditAction.class, RETURNS_SELF);
    private final LocalDate date = LocalDate.of(2026, 10, 24);

    @BeforeEach
    void setup() {
        when(event.isFromGuild()).thenReturn(true);
        var guild = mock(Guild.class);
        when(guild.getId()).thenReturn("200");
        when(event.getGuild()).thenReturn(guild);
        when(event.getMember()).thenReturn(mock(Member.class));
        var user = mock(User.class);
        when(user.getId()).thenReturn("300");
        when(event.getUser()).thenReturn(user);
        var id = mock(OptionMapping.class);
        when(id.getAsString()).thenReturn("1");
        when(event.getOption("id")).thenReturn(id);
        when(event.deferReply(true)).thenReturn(mock(ReplyCallbackAction.class));
        when(hook.editOriginal(any(MessageEditData.class))).thenReturn(edit);
    }

    private MessageEditData run(String command) {
        when(event.getSubcommandName()).thenReturn(command);
        handler.handle(event);
        verify(event).deferReply(true);
        handler.afterAcknowledgement(event, hook);
        var payload = ArgumentCaptor.forClass(MessageEditData.class);
        verify(hook).editOriginal(payload.capture());
        verify(edit).queue(isNull(), any());
        var message = payload.getValue();
        assertThat(message.getAllowedMentions()).isEmpty();
        assertThat(message.getMentionedRoles()).isEmpty();
        assertThat(message.getEmbeds()).hasSize(1);
        return message;
    }

    @Test
    void todayIsAPrivateCardWithButtonsAndTimezone() {
        when(today.today(1L, "200")).thenReturn(new ChallengeTodayDto(1L, "Practice @everyone", 5, date,
                "Africa/Casablanca", "*".repeat(300), "contains-duplicate", ProblemDifficulty.EASY,
                Instant.parse("2026-10-24T23:00:00Z")));
        var message = run("today");
        var embed = message.getEmbeds().getFirst();
        assertThat(embed.getTitle()).hasSizeLessThanOrEqualTo(256);
        assertThat(embed.getUrl()).isEqualTo("https://leetcode.com/problems/contains-duplicate/");
        assertThat(embed.getFooter().getText()).contains("2026-10-24", "Africa/Casablanca", "Day 5");
        var buttons = message.getComponents().getFirst().asActionRow().getButtons();
        assertThat(buttons).hasSize(3);
        assertThat(buttons.getFirst().getCustomId()).isEqualTo("dailycode:challenge:join:1");
        assertThat(buttons.getLast().getUrl()).isEqualTo(embed.getUrl());
        verifyNoInteractions(completion);
    }

    @ParameterizedTest
    @EnumSource(DailyCompletionOutcome.class)
    void checkDisplaysEachOutcomeAsACard(DailyCompletionOutcome outcome) {
        when(completion.checkToday(1L, "200", "300")).thenReturn(new DailyCompletionResultDto(
                1L, 2L, date, null, outcome, null, null));
        var message = run("check");
        assertThat(message.getEmbeds().getFirst().getTitle()).isEqualTo(switch (outcome) {
            case COMPLETED -> "Challenge completed!";
            case ALREADY_COMPLETED -> "Already completed";
            case NO_MATCH -> "No accepted submission found";
        });
        verify(completion).checkToday(1L, "200", "300");
        verifyNoInteractions(today);
    }

    @Test
    void checkingAnExplicitDateKeepsThatDateOnTheCard() {
        var option = mock(OptionMapping.class);
        when(option.getAsString()).thenReturn("2026-10-23");
        when(event.getOption("date")).thenReturn(option);
        when(completion.check(1L, "200", "300", date.minusDays(1))).thenReturn(
                new DailyCompletionResultDto(1L, 2L, date.minusDays(1), 3L,
                        DailyCompletionOutcome.ALREADY_COMPLETED, "123", Instant.parse("2026-10-23T12:00:00Z")));
        var message = run("check");
        assertThat(message.getEmbeds().getFirst().getFields()).anySatisfy(field -> {
            assertThat(field.getName()).isEqualTo("Assignment date");
            assertThat(field.getValue()).isEqualTo("2026-10-23");
        });
        verify(completion, never()).checkToday(anyLong(), anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void waitingCardExplainsWhetherPostingTimeHasPassed(boolean passed) {
        when(today.today(1L, "200")).thenThrow(new ChallengeTodayException(
                ChallengeTodayException.Reason.NO_ASSIGNMENT, new ChallengeTodayException.Schedule(
                        date, "Africa/Casablanca", Instant.parse("2026-10-24T10:00:00Z"), passed)));
        var embed = run("today").getEmbeds().getFirst();
        assertThat(embed.getDescription()).contains("midnight", passed ? "posting time has passed" : "scheduled posting time");
        assertThat(embed.getFields()).anySatisfy(field ->
                assertThat(field.getValue()).contains("2026-10-24", "Africa/Casablanca"));
    }

    @Test
    void waitingCardExplainsDisabledAutomation() {
        when(today.today(1L, "200")).thenThrow(new ChallengeTodayException(
                ChallengeTodayException.Reason.NO_ASSIGNMENT,
                new ChallengeTodayException.Schedule(date, "UTC", null, false)));
        assertThat(run("today").getEmbeds().getFirst().getDescription())
                .contains("Automatic posting is disabled", "/challenge automate");
    }

    @Test
    void checkErrorsAreAlsoCardsWithoutLeakingServerData() {
        when(completion.checkToday(1L, "200", "300")).thenThrow(
                new DailyCompletionException(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        var embed = run("check").getEmbeds().getFirst();
        assertThat(embed.getTitle()).isEqualTo("Unable to check submission");
        assertThat(embed.getDescription()).isEqualTo("That challenge was not found in this server.");
    }
}
