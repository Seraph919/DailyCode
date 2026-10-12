package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        doAnswer(invocation -> {
            Consumer<Command> success = invocation.getArgument(0);
            success.accept(command);
            return null;
        }).when(action).queue(any(), any());

        listener.onReady(readyEvent(jda));

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
        doAnswer(invocation -> {
            Consumer<Throwable> failure = invocation.getArgument(1);
            failure.accept(new IllegalStateException("Missing Access"));
            return null;
        }).when(action).queue(any(), any());

        listener.onReady(readyEvent(jda));

        assertThat(output.getOut()).contains("Could not register /ping", "Missing Access")
                .doesNotContain("Registered /ping");
    }

    @Test
    void repliesPongToPingInTheTestGuild() {
        SlashCommandInteractionEvent event = guildInteraction("ping", GUILD_ID);
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class);
        when(event.reply("Pong!")).thenReturn(reply);

        listener.onSlashCommandInteraction(event);

        verify(event).reply("Pong!");
        verify(reply).queue(any(), any());
    }

    @Test
    void ignoresOtherCommands() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn("challenge");

        listener.onSlashCommandInteraction(event);

        verify(event, never()).reply(anyString());
    }

    @Test
    void ignoresPingFromOtherGuilds() {
        SlashCommandInteractionEvent event = guildInteraction("ping", "111111111111111111");

        listener.onSlashCommandInteraction(event);

        verify(event, never()).reply(anyString());
    }

    @Test
    void ignoresDirectMessages() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        when(event.getName()).thenReturn("ping");
        when(event.isFromGuild()).thenReturn(false);

        listener.onSlashCommandInteraction(event);

        verify(event, never()).reply(anyString());
    }

    @Test
    void reportsReplyFailure(CapturedOutput output) {
        SlashCommandInteractionEvent event = guildInteraction("ping", GUILD_ID);
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class);
        when(event.reply("Pong!")).thenReturn(reply);
        doAnswer(invocation -> {
            Consumer<Throwable> failure = invocation.getArgument(1);
            failure.accept(new IllegalStateException("Unknown interaction"));
            return null;
        }).when(reply).queue(any(), any());

        listener.onSlashCommandInteraction(event);

        assertThat(output.getOut()).contains("Could not reply to /ping", "Unknown interaction");
    }

    private ReadyEvent readyEvent(JDA jda) {
        ReadyEvent event = mock(ReadyEvent.class);
        when(event.getJDA()).thenReturn(jda);
        return event;
    }

    private SlashCommandInteractionEvent guildInteraction(String commandName, String guildId) {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        when(event.getName()).thenReturn(commandName);
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn(guildId);
        return event;
    }
}
