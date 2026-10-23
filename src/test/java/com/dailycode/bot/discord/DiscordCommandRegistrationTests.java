package com.dailycode.bot.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class DiscordCommandRegistrationTests {
    static Stream<Arguments> commands() {
        return Stream.of(
                Arguments.of("ping", (Function<String, ListenerAdapter>) PingCommandListener::new),
                Arguments.of("leetcode", (Function<String, ListenerAdapter>) LeetCodeCommandListener::new),
                Arguments.of("challenge", (Function<String, ListenerAdapter>) ChallengeCommandListener::new));
    }

    @ParameterizedTest
    @MethodSource("commands")
    void registersGloballyWithoutATestGuild(String name, Function<String, ListenerAdapter> listener) {
        JDA jda = mock(JDA.class);
        var action = mock(CommandCreateAction.class);
        when(jda.upsertCommand(any(CommandData.class))).thenReturn(action);

        listener.apply("").onReady(ready(jda));

        ArgumentCaptor<CommandData> data = ArgumentCaptor.forClass(CommandData.class);
        verify(jda).upsertCommand(data.capture());
        assertThat(data.getValue().getName()).isEqualTo(name);
        assertThat(data.getValue().getContexts()).containsExactly(InteractionContextType.GUILD);
        assertThat(data.getValue().getIntegrationTypes()).containsExactly(IntegrationType.GUILD_INSTALL);
        verify(action).queue(any(), any());
        // Global commands cover new installations without a guild-join registration step.
        verifyNoMoreInteractions(jda);
    }

    @ParameterizedTest
    @MethodSource("commands")
    void testGuildMirrorsCommandsWithoutDisablingGlobalRegistration(String name, Function<String, ListenerAdapter> listener) {
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        var global = mock(CommandCreateAction.class);
        var local = mock(CommandCreateAction.class);
        when(jda.upsertCommand(any(CommandData.class))).thenReturn(global);
        when(jda.getGuildById("100")).thenReturn(guild);
        when(guild.upsertCommand(any(CommandData.class))).thenReturn(local);

        listener.apply("100").onReady(ready(jda));

        verify(jda).upsertCommand(argThat(command -> command.getName().equals(name)));
        verify(guild).upsertCommand(argThat(command -> command.getName().equals(name)));
        verify(global).queue(any(), any());
        verify(local).queue(any(), any());
        verify(guild, never()).updateCommands();
    }

    @ParameterizedTest
    @MethodSource("commands")
    void unavailableTestGuildDoesNotBlockGlobalCommands(String name, Function<String, ListenerAdapter> listener) {
        JDA jda = mock(JDA.class);
        var action = mock(CommandCreateAction.class);
        when(jda.upsertCommand(any(CommandData.class))).thenReturn(action);

        listener.apply("100").onReady(ready(jda));

        verify(jda).upsertCommand(argThat(command -> command.getName().equals(name)));
        verify(action).queue(any(), any());
    }

    @ParameterizedTest
    @MethodSource("commands")
    void invalidTestGuildDoesNotBlockGlobalCommands(String name, Function<String, ListenerAdapter> listener) {
        JDA jda = mock(JDA.class);
        var action = mock(CommandCreateAction.class);
        when(jda.upsertCommand(any(CommandData.class))).thenReturn(action);
        when(jda.getGuildById("invalid")).thenThrow(new IllegalArgumentException("Invalid ID"));

        listener.apply("invalid").onReady(ready(jda));

        verify(action).queue(any(), any());
    }

    @ParameterizedTest
    @MethodSource("commands")
    void reportsRegistrationFailureWithoutClaimingSuccess(String name, Function<String, ListenerAdapter> listener,
                                                         CapturedOutput output) {
        JDA jda = mock(JDA.class);
        var action = mock(CommandCreateAction.class);
        when(jda.upsertCommand(any(CommandData.class))).thenReturn(action);
        listener.apply("").onReady(ready(jda));
        ArgumentCaptor<Consumer<Throwable>> failure = ArgumentCaptor.captor();
        verify(action).queue(any(), failure.capture());
        failure.getValue().accept(new IllegalStateException("Failed"));
        assertThat(output.getOut()).contains("Could not register /" + name + " globally")
                .doesNotContain("Registered /" + name + " globally");
    }

    private ReadyEvent ready(JDA jda) {
        ReadyEvent event = mock(ReadyEvent.class);
        when(event.getJDA()).thenReturn(jda);
        return event;
    }
}
