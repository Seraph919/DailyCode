package com.dailycode.bot.discord;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import static com.dailycode.bot.discord.CardMatchers.*;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class SlashCommandRouterTests {

    private static final String GUILD_ID = "1260299157571239936";
    private final SlashCommandRouter router = new SlashCommandRouter(List.of(new PingCommandHandler()));

    @Test
    void routesPingToItsHandlerAndQueuesOneReply() {
        SlashCommandInteractionEvent event = event("ping", GUILD_ID);
        ReplyCallbackAction reply = reply(event, "Pong!");

        router.onSlashCommandInteraction(event);

        verify(event).reply(cardContaining("Pong!"));
        verify(reply).queue(any(), any());
        verify(reply, never()).setEphemeral(anyBoolean());
    }

    @Test
    void selectsAHandlerByNameRatherThanListPosition() {
        SlashCommandHandler first = mock(SlashCommandHandler.class);
        SlashCommandHandler second = mock(SlashCommandHandler.class);
        when(first.name()).thenReturn("first");
        when(second.name()).thenReturn("second");
        SlashCommandInteractionEvent event = event("second", GUILD_ID);
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class);
        when(second.handle(event)).thenReturn(reply);

        new SlashCommandRouter(List.of(first, second)).onSlashCommandInteraction(event);

        verify(second).handle(event);
        verify(first, never()).handle(any());
        verify(reply).queue(any(), any());
    }

    @Test
    void unknownCommandsGetAPrivateResponse() {
        SlashCommandInteractionEvent event = event("retired-command", GUILD_ID);
        ReplyCallbackAction reply = reply(event, "This command is not available yet.");

        router.onSlashCommandInteraction(event);

        verify(reply).setEphemeral(true);
        verify(reply).queue(any(), any());
    }

    @Test
    void handlerFailureGetsAGenericPrivateResponse(CapturedOutput output) {
        SlashCommandInteractionEvent event = event("broken", GUILD_ID);
        SlashCommandHandler handler = mock(SlashCommandHandler.class);
        when(handler.name()).thenReturn("broken");
        when(handler.handle(event)).thenThrow(new IllegalStateException("Internal database detail"));
        ReplyCallbackAction reply = reply(event, "Something went wrong. Please try again.");

        new SlashCommandRouter(List.of(handler)).onSlashCommandInteraction(event);

        verify(event).reply(cardContaining("Something went wrong. Please try again."));
        verify(event, never()).reply(cardContaining("Internal database detail"));
        verify(reply).setEphemeral(true);
        verify(reply).queue(any(), any());
        assertThat(output.getOut()).contains("Failed to handle /broken.", "Internal database detail");
    }

    @Test
    void rejectsDuplicateHandlerNames() {
        try {
            new SlashCommandRouter(List.of(new PingCommandHandler(), new PingCommandHandler()));
            fail("Expected duplicate handler names to be rejected");
        } catch (IllegalStateException exception) {
            assertThat(exception).hasMessage("Duplicate slash command handler: ping");
        }
    }

    @Test
    void routesCommandsFromAnotherGuild() {
        SlashCommandInteractionEvent event = event("ping", "111111111111111111");
        ReplyCallbackAction reply = reply(event, "Pong!");

        router.onSlashCommandInteraction(event);

        verify(event).reply(cardContaining("Pong!"));
        verify(reply).queue(any(), any());
    }

    @Test
    void ignoresDirectMessages() {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);

        router.onSlashCommandInteraction(event);

        verify(event, never()).reply(any(MessageCreateData.class));
    }

    @Test
    void doesNotAcknowledgeAnInteractionTwice() {
        SlashCommandInteractionEvent event = event("ping", GUILD_ID);
        when(event.isAcknowledged()).thenReturn(true);

        router.onSlashCommandInteraction(event);

        verify(event, never()).reply(any(MessageCreateData.class));
    }

    @Test
    void doesNotSendAnErrorIfAFailingHandlerAlreadyAcknowledged() {
        SlashCommandInteractionEvent event = event("broken", GUILD_ID);
        when(event.isAcknowledged()).thenReturn(false, true);
        SlashCommandHandler handler = mock(SlashCommandHandler.class);
        when(handler.name()).thenReturn("broken");
        when(handler.handle(event)).thenThrow(new IllegalStateException("Handler violated reply ownership"));

        new SlashCommandRouter(List.of(handler)).onSlashCommandInteraction(event);

        verify(event, never()).reply(any(MessageCreateData.class));
    }

    @Test
    void logsAsyncDeliveryFailureWithoutRetrying(CapturedOutput output) {
        SlashCommandInteractionEvent event = event("ping", GUILD_ID);
        ReplyCallbackAction reply = reply(event, "Pong!");

        router.onSlashCommandInteraction(event);

        ArgumentCaptor<Consumer<Throwable>> failureCallback = ArgumentCaptor.captor();
        verify(reply).queue(any(), failureCallback.capture());
        failureCallback.getValue().accept(new IllegalStateException("Unknown interaction"));

        verify(event).reply(cardContaining("Pong!"));
        verify(event, times(1)).reply(any(MessageCreateData.class));
        verify(reply, times(1)).queue(any(), any());
        assertThat(output.getOut()).contains("Could not deliver reply for /ping.");
    }

    @Test
    void logsQueueFailureWithoutRetrying(CapturedOutput output) {
        SlashCommandInteractionEvent event = event("ping", GUILD_ID);
        ReplyCallbackAction reply = reply(event, "Pong!");
        doThrow(new IllegalStateException("JDA is shutting down")).when(reply).queue(any(), any());

        router.onSlashCommandInteraction(event);

        verify(event, times(1)).reply(any(MessageCreateData.class));
        verify(reply, times(1)).queue(any(), any());
        assertThat(output.getOut()).contains("Could not queue reply for /ping.");
    }

    private SlashCommandInteractionEvent event(String name, String guildId) {
        SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
        Guild guild = mock(Guild.class);
        when(event.getName()).thenReturn(name);
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn(guildId);
        return event;
    }

    private ReplyCallbackAction reply(SlashCommandInteractionEvent event, String message) {
        ReplyCallbackAction reply = mock(ReplyCallbackAction.class, RETURNS_SELF);
        when(event.reply(cardContaining(message))).thenReturn(reply);
        return reply;
    }
}
