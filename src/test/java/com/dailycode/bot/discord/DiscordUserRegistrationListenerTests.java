package com.dailycode.bot.discord;

import com.dailycode.bot.application.user.DiscordUserService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class DiscordUserRegistrationListenerTests {

    private static final String GUILD_ID = "1260299157571239936";
    private static final String USER_ID = "123456789012345678";
    private final DiscordUserService service = mock(DiscordUserService.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);
    private final DiscordUserRegistrationListener listener =
            new DiscordUserRegistrationListener(GUILD_ID, service, executor);

    @Test
    void queuesIdentityFromTheEventWithoutDoingDatabaseWorkOnTheEventThread(CapturedOutput output) {
        SlashCommandInteractionEvent event = event(GUILD_ID, "alice");

        listener.onSlashCommandInteraction(event);

        verifyNoInteractions(service);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        // Run the captured job ourselves so the test needs no threads or sleeps.
        task.getValue().run();

        verify(service).register(USER_ID, "alice");
        verify(event, never()).reply(anyString());
        verify(event, never()).deferReply();
        assertThat(output.getOut()).contains("Saved Discord user " + USER_ID);
    }

    @Test
    void registersEvenWhenTheRouterAlreadyAcknowledgedTheCommand() {
        SlashCommandInteractionEvent event = event(GUILD_ID, "alice");
        when(event.isAcknowledged()).thenReturn(true);

        listener.onSlashCommandInteraction(event);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        task.getValue().run();
        verify(service).register(USER_ID, "alice");
    }

    @Test
    void passesRepeatedCommandsAndUpdatedUsernamesToTheExistingIdempotentService() {
        listener.onSlashCommandInteraction(event(GUILD_ID, "alice"));
        listener.onSlashCommandInteraction(event(GUILD_ID, "alice-renamed"));

        ArgumentCaptor<Runnable> tasks = ArgumentCaptor.forClass(Runnable.class);
        verify(executor, times(2)).execute(tasks.capture());
        List<Runnable> queuedTasks = tasks.getAllValues();
        for (Runnable task : queuedTasks) {
            task.run();
        }

        verify(service).register(USER_ID, "alice");
        verify(service).register(USER_ID, "alice-renamed");
    }

    @Test
    void ignoresDirectMessages() {
        listener.onSlashCommandInteraction(mock(SlashCommandInteractionEvent.class));

        verifyNoInteractions(executor, service);
    }

    @Test
    void ignoresOtherGuilds() {
        listener.onSlashCommandInteraction(event("111111111111111111", "alice"));

        verifyNoInteractions(executor, service);
    }

    @Test
    void ignoresBots() {
        SlashCommandInteractionEvent event = event(GUILD_ID, "alice");
        when(event.getUser().isBot()).thenReturn(true);

        listener.onSlashCommandInteraction(event);

        verifyNoInteractions(executor, service);
    }

    @Test
    void logsAFullOrStoppedQueueWithoutBlockingTheEventThread(CapturedOutput output) {
        doThrow(new TaskRejectedException("Queue full")).when(executor).execute(any(Runnable.class));
        SlashCommandInteractionEvent event = event(GUILD_ID, "alice");

        listener.onSlashCommandInteraction(event);

        verifyNoInteractions(service);
        verify(event, never()).reply(anyString());
        assertThat(output.getOut()).contains("Could not queue registration for Discord user " + USER_ID);
    }

    @Test
    void logsDatabaseFailureWithoutSendingAnotherReply(CapturedOutput output) {
        doThrow(new IllegalStateException("Database unavailable")).when(service).register(USER_ID, "alice");
        SlashCommandInteractionEvent event = event(GUILD_ID, "alice");
        listener.onSlashCommandInteraction(event);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());

        task.getValue().run();

        verify(event, never()).reply(anyString());
        assertThat(output.getOut()).contains("Could not save Discord user " + USER_ID, "Database unavailable");
    }

    private SlashCommandInteractionEvent event(String guildId, String username) {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        User user = mock(User.class);
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn(guildId);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn(username);
        return event;
    }
}
