package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeLeaderboardService;
import com.dailycode.bot.application.challenge.DailyCompletionException;
import com.dailycode.bot.application.challenge.dto.ChallengeLeaderboardDto;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.entities.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChallengeLeaderboardCommandTests {
    private final ChallengeLeaderboardService leaderboard = mock(ChallengeLeaderboardService.class);
    private final ChallengeCommandHandler handler = new ChallengeCommandHandler(
            null, null, null, null, null, null, null, leaderboard, new SyncTaskExecutor());
    private final SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    private final InteractionHook hook = mock(InteractionHook.class);
    private final WebhookMessageEditAction<Message> edit = mock(WebhookMessageEditAction.class, RETURNS_SELF);

    @BeforeEach
    void setup() {
        when(event.getSubcommandName()).thenReturn("leaderboard");
        when(event.isFromGuild()).thenReturn(true);
        var guild = mock(Guild.class);
        when(guild.getId()).thenReturn("200");
        when(event.getGuild()).thenReturn(guild);
        // Ordinary members can view the leaderboard; Manage Server is not required.
        when(event.getMember()).thenReturn(mock(Member.class));
        var id = mock(OptionMapping.class);
        when(id.getAsString()).thenReturn("1");
        when(event.getOption("id")).thenReturn(id);
        when(event.deferReply(true)).thenReturn(mock(ReplyCallbackAction.class));
        when(event.reply(anyString())).thenReturn(mock(ReplyCallbackAction.class, RETURNS_SELF));
        when(hook.editOriginal(anyString())).thenReturn(edit);
    }

    @Test
    void displaysRankedResultsPrivatelyWithoutPingingParticipants() {
        when(leaderboard.leaderboard(1L, "200", 1)).thenReturn(new ChallengeLeaderboardDto(
                1L, "*Practice* @everyone", 1, 1, 2,
                List.of(new ChallengeLeaderboardDto.Entry(1, "300", 3),
                        new ChallengeLeaderboardDto.Entry(1, "301", 3))));
        runCommand();
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(hook).editOriginal(message.capture());
        assertThat(message.getValue()).contains("#1 · <@300> — 3 completed", "#1 · <@301> — 3 completed",
                "Page 1/1", "\\*Practice\\*", "recorded completions").hasSizeLessThan(2000);
        verify(edit).setAllowedMentions(List.of());
    }

    @Test
    void usesTheRequestedPageAndExplainsOutOfRangePages() {
        var page = mock(OptionMapping.class);
        when(page.getAsLong()).thenReturn(3L);
        when(event.getOption("page")).thenReturn(page);
        when(leaderboard.leaderboard(1L, "200", 3)).thenReturn(new ChallengeLeaderboardDto(1L, "Practice", 3, 2, 12, List.of()));
        runCommand();
        verify(hook).editOriginal("That leaderboard page does not exist. Choose a page from 1 to 2.");
    }

    @Test
    void explainsAnEmptyLeaderboard() {
        when(leaderboard.leaderboard(1L, "200", 1)).thenReturn(new ChallengeLeaderboardDto(1L, "Practice", 1, 1, 0, List.of()));
        runCommand();
        verify(hook).editOriginal(contains("No participants yet. Join with /challenge join."));
    }

    @Test
    void rejectsInvalidPagesBeforeQueuingWork() {
        var page = mock(OptionMapping.class);
        when(page.getAsLong()).thenReturn(0L);
        when(event.getOption("page")).thenReturn(page);
        handler.handle(event);
        verify(event).reply("Page must be between 1 and 1000000.");
        verify(event, never()).deferReply(anyBoolean());
        verifyNoInteractions(leaderboard);
    }

    @Test
    void hidesChallengesFromAnotherServer() {
        when(leaderboard.leaderboard(1L, "200", 1)).thenThrow(
                new DailyCompletionException(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        runCommand();
        verify(hook).editOriginal("That challenge was not found in this server.");
    }

    @Test
    void handlesDatabaseFailuresWithoutExposingDetails() {
        when(leaderboard.leaderboard(1L, "200", 1)).thenThrow(new IllegalStateException("Database detail"));
        runCommand();
        verify(hook).editOriginal("Could not load the leaderboard. Please try again shortly.");
    }

    private void runCommand() {
        handler.handle(event);
        verify(event).deferReply(true);
        handler.afterAcknowledgement(event, hook);
    }
}
