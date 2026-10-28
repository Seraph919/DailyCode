package com.dailycode.bot.discord;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import static com.dailycode.bot.discord.CardMatchers.*;

import com.dailycode.bot.application.challenge.ChallengeJoinException;
import com.dailycode.bot.application.challenge.ChallengeJoinService;
import com.dailycode.bot.application.challenge.ChallengeProgressService;
import com.dailycode.bot.application.challenge.DailyCompletionException;
import com.dailycode.bot.application.challenge.dto.ChallengeParticipantDto;
import com.dailycode.bot.application.challenge.dto.ChallengeProgressDto;
import com.dailycode.bot.application.user.DiscordUserService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChallengeButtonListenerTests {
    private final DiscordUserService users = mock(DiscordUserService.class);
    private final ChallengeJoinService joins = mock(ChallengeJoinService.class);
    private final ChallengeProgressService progress = mock(ChallengeProgressService.class);
    private final TaskExecutor executor = mock(TaskExecutor.class);
    private final ChallengeCommandHandler handler = new ChallengeCommandHandler(
            users, null, joins, null, null, null, progress, null, executor);
    private final ChallengeButtonListener listener = new ChallengeButtonListener(handler);
    private final ButtonInteractionEvent event = mock(ButtonInteractionEvent.class);
    private final ReplyCallbackAction acknowledgement = mock(ReplyCallbackAction.class, RETURNS_SELF);
    private final InteractionHook hook = mock(InteractionHook.class);
    @SuppressWarnings("unchecked")
    private final WebhookMessageEditAction<Message> response = mock(WebhookMessageEditAction.class, RETURNS_SELF);

    @BeforeEach
    void repliesCanBeDelivered() {
        when(hook.editOriginal(any(MessageEditData.class))).thenReturn(response);
    }

    private void prepare(String action) {
        when(event.getComponentId()).thenReturn(ChallengeButtonListener.id(action, 42));
        when(event.isFromGuild()).thenReturn(true);
        when(event.getMember()).thenReturn(mock(Member.class));
        Guild guild = mock(Guild.class);
        when(guild.getId()).thenReturn("200");
        when(event.getGuild()).thenReturn(guild);
        User user = mock(User.class);
        when(user.getId()).thenReturn("300");
        when(user.getName()).thenReturn("coder");
        when(event.getUser()).thenReturn(user);
        when(event.deferReply(true)).thenReturn(acknowledgement);
    }

    @SuppressWarnings("unchecked")
    private void acknowledgeAndRun() {
        doAnswer(call -> {
            // No database work may run until Discord acknowledges the interaction.
            verifyNoInteractions(users, joins, progress, executor);
            ((Consumer<InteractionHook>) call.getArgument(0)).accept(hook);
            return null;
        }).when(acknowledgement).queue(any(), any());
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(executor).execute(any());
        listener.onButtonInteraction(event);
    }

    @Test
    void joinUsesClickingUserAndServerAfterPrivateAcknowledgement() {
        prepare("join");
        when(joins.join(42L, "200", "300")).thenReturn(
                new ChallengeParticipantDto(1L, 42L, "Practice", "200", "300", Instant.now()));
        acknowledgeAndRun();
        verify(event).deferReply(true);
        var order = inOrder(users, joins);
        order.verify(users).register("300", "coder");
        order.verify(joins).join(42L, "200", "300");
        verify(hook).editOriginal(editContaining("You joined challenge #42: Practice."));

        verify(response).queue(isNull(), any());
    }

    @Test
    void statusUsesCurrentSavedProgressForTheClickingMember() {
        prepare("status");
        when(progress.status(42L, "200", "300", 1)).thenReturn(new ChallengeProgressDto(
                42L, "Practice", "UTC", LocalDate.of(2026, 10, 23), "Completed", true,
                LocalTime.NOON, 3, 1, 1, List.of()));
        acknowledgeAndRun();
        verify(event).deferReply(true);
        verify(progress).status(42L, "200", "300", 1);
        verify(hook).editOriginal(editContaining("Saved completions: 3"));
        verifyNoInteractions(users, joins);
    }

    @ParameterizedTest
    @ValueSource(strings = {"join", "status"})
    void anotherServersChallengeIsRejectedByTheScopedService(String action) {
        prepare(action);
        when(joins.join(42L, "200", "300")).thenThrow(
                new ChallengeJoinException(ChallengeJoinException.Reason.CHALLENGE_NOT_FOUND));
        when(progress.status(42L, "200", "300", 1)).thenThrow(
                new DailyCompletionException(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        acknowledgeAndRun();
        verify(hook).editOriginal(editContaining("That challenge was not found in this server."));
    }

    @Test
    void joinStillRequiresVerification() {
        prepare("join");
        when(joins.join(42L, "200", "300")).thenThrow(
                new ChallengeJoinException(ChallengeJoinException.Reason.ACCOUNT_NOT_VERIFIED));
        acknowledgeAndRun();
        verify(hook).editOriginal(editContaining("Verify your LeetCode account before joining a challenge."));
    }

    @Test
    void statusExplainsWhenTheUserHasNotJoined() {
        prepare("status");
        when(progress.status(42L, "200", "300", 1)).thenThrow(
                new DailyCompletionException(DailyCompletionException.Reason.NOT_JOINED));
        acknowledgeAndRun();
        verify(hook).editOriginal(editContaining("Join this challenge to view your progress."));
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullQueueGetsAPrivateBusyResponse() {
        prepare("join");
        doThrow(new TaskRejectedException("full")).when(executor).execute(any());
        doAnswer(call -> {
            ((Consumer<InteractionHook>) call.getArgument(0)).accept(hook);
            return null;
        }).when(acknowledgement).queue(any(), any());
        listener.onButtonInteraction(event);
        verify(hook).editOriginal(editContaining("The bot is busy. Please try again shortly."));
        verifyNoInteractions(users, joins, progress);
    }

    @ParameterizedTest
    @ValueSource(strings = {"join:0", "join:-1", "status:9223372036854775808", "delete:42", "join:42:extra"})
    void invalidButtonIdsNeverReachServices(String suffix) {
        when(event.getComponentId()).thenReturn("dailycode:challenge:" + suffix);
        when(event.reply(any(MessageCreateData.class))).thenReturn(acknowledgement);
        listener.onButtonInteraction(event);
        verify(acknowledgement).setEphemeral(true);
        verify(event, never()).deferReply(anyBoolean());
        verifyNoInteractions(users, joins, progress, executor);
    }

    @Test
    void ignoresOtherFeaturesAndAlreadyAcknowledgedButtons() {
        when(event.getComponentId()).thenReturn("another-feature:42");
        listener.onButtonInteraction(event);
        when(event.getComponentId()).thenReturn(ChallengeButtonListener.id("join", 42));
        when(event.isAcknowledged()).thenReturn(true);
        listener.onButtonInteraction(event);
        verify(event, never()).deferReply(anyBoolean());
        verifyNoInteractions(users, joins, progress, executor);
    }

    @Test
    void rejectsButtonsOutsideAServer() {
        when(event.getComponentId()).thenReturn(ChallengeButtonListener.id("join", 42));
        when(event.reply(any(MessageCreateData.class))).thenReturn(acknowledgement);
        listener.onButtonInteraction(event);
        verify(event).reply(cardContaining("Use challenge buttons inside a server."));
        verify(acknowledgement).setEphemeral(true);
        verifyNoInteractions(users, joins, progress, executor);
    }
}
