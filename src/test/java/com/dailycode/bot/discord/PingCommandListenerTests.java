package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class PingCommandListenerTests {

    private static final String GUILD_ID = "1260299157571239936";
    private final PingCommandListener listener = new PingCommandListener(GUILD_ID);

    @Test
    void upsertsOnlyPingInTheConfiguredGuild(CapturedOutput output) {
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        CommandCreateAction action = mock(CommandCreateAction.class);
        Command command = mock(Command.class);
        when(jda.getGuildById(GUILD_ID)).thenReturn(guild);
        when(guild.upsertCommand("ping", "Check whether DailyCode is responding.")).thenReturn(action);
        when(command.getId()).thenReturn("987654321");

        listener.onReady(readyEvent(jda));

        ArgumentCaptor<Consumer<Command>> successCallback = ArgumentCaptor.captor();
        verify(action).queue(successCallback.capture(), any());
        successCallback.getValue().accept(command);

        verify(jda).getGuildById(GUILD_ID);
        verify(guild).upsertCommand("ping", "Check whether DailyCode is responding.");
        verify(guild, never()).updateCommands();
        verifyNoMoreInteractions(jda, guild);
        assertThat(output.getOut()).contains("Registered /ping in guild " + GUILD_ID);
    }

    @Test
    void skipsRegistrationWhenNoServerIsConfigured(CapturedOutput output) {
        ReadyEvent event = mock(ReadyEvent.class);

        new PingCommandListener("").onReady(event);

        verifyNoInteractions(event);
        assertThat(output.getOut()).contains("set DISCORD_TEST_GUILD_ID");
    }

    @Test
    void reportsAnInvalidServerId(CapturedOutput output) {
        JDA jda = mock(JDA.class);
        when(jda.getGuildById("not-an-id")).thenThrow(new IllegalArgumentException("Invalid ID"));

        new PingCommandListener("not-an-id").onReady(readyEvent(jda));

        assertThat(output.getOut()).contains("must be a numeric Discord server ID");
    }

    @Test
    void reportsAnUnavailableGuild(CapturedOutput output) {
        JDA jda = mock(JDA.class);

        listener.onReady(readyEvent(jda));

        assertThat(output.getOut()).contains("guild " + GUILD_ID + " is unavailable");
    }

    @Test
    void reportsRegistrationFailureWithoutClaimingSuccess(CapturedOutput output) {
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        CommandCreateAction action = mock(CommandCreateAction.class);
        when(jda.getGuildById(GUILD_ID)).thenReturn(guild);
        when(guild.upsertCommand("ping", "Check whether DailyCode is responding.")).thenReturn(action);

        listener.onReady(readyEvent(jda));

        ArgumentCaptor<Consumer<Throwable>> failureCallback = ArgumentCaptor.captor();
        verify(action).queue(any(), failureCallback.capture());
        failureCallback.getValue().accept(new IllegalStateException("Missing Access"));

        assertThat(output.getOut()).contains("Could not register /ping", "Missing Access")
                .doesNotContain("Registered /ping");
    }

    private ReadyEvent readyEvent(JDA jda) {
        ReadyEvent event = mock(ReadyEvent.class);
        when(event.getJDA()).thenReturn(jda);
        return event;
    }

}
