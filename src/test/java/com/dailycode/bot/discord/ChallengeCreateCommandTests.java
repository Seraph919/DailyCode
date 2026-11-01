package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeService;
import com.dailycode.bot.application.challenge.dto.ChallengeDto;
import com.dailycode.bot.application.user.DiscordUserService;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import static com.dailycode.bot.discord.CardMatchers.cardContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChallengeCreateCommandTests {

    private final DiscordUserService users = mock(DiscordUserService.class);
    private final ChallengeService challenges = mock(ChallengeService.class);
    private final ChallengeCommandHandler handler = new ChallengeCommandHandler(
            users, challenges, null, null, null, null, null, null, new SyncTaskExecutor());
    private final SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    private final InteractionHook hook = mock(InteractionHook.class, RETURNS_DEEP_STUBS);
    private final Guild guild = mock(Guild.class);
    private final Member member = mock(Member.class);
    private final User user = mock(User.class);

    @BeforeEach
    void setup() {
        when(event.getSubcommandName()).thenReturn("create");
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("100");
        when(event.getMember()).thenReturn(member);
        when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("200");
        when(user.getName()).thenReturn("alice");

        OptionMapping nameOpt = mock(OptionMapping.class);
        when(nameOpt.getAsString()).thenReturn("My Challenge");
        when(event.getOption("name")).thenReturn(nameOpt);

        OptionMapping endOpt = mock(OptionMapping.class);
        when(endOpt.getAsString()).thenReturn("2026-12-31");
        when(event.getOption("end_date")).thenReturn(endOpt);

        when(event.deferReply(true)).thenReturn(mock(ReplyCallbackAction.class));
        when(event.reply(any(MessageCreateData.class))).thenReturn(mock(ReplyCallbackAction.class, RETURNS_SELF));

        when(challenges.create(anyString(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(new ChallengeDto(1L, "100", "My Challenge", "200",
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), "Africa/Casablanca",
                        ChallengeStatus.SCHEDULED, Instant.now(), ProblemDifficulty.EASY));
    }

    @Test
    void createPassesDifficultyToService() {
        OptionMapping diffOpt = mock(OptionMapping.class);
        when(diffOpt.getAsString()).thenReturn("MEDIUM");
        when(event.getOption("difficulty")).thenReturn(diffOpt);

        handler.handle(event);
        verify(event).deferReply(true);
        handler.afterAcknowledgement(event, hook);

        verify(challenges).create(eq("100"), eq("200"), eq("My Challenge"),
                isNull(), eq(LocalDate.of(2026, 12, 31)), isNull(), eq(ProblemDifficulty.MEDIUM));
    }

    @Test
    void createDefaultsDifficultyToEasy() {
        when(event.getOption("difficulty")).thenReturn(null);

        handler.handle(event);
        verify(event).deferReply(true);
        handler.afterAcknowledgement(event, hook);

        verify(challenges).create(eq("100"), eq("200"), eq("My Challenge"),
                isNull(), eq(LocalDate.of(2026, 12, 31)), isNull(), eq(ProblemDifficulty.EASY));
    }

    @Test
    void createRejectsInvalidDifficulty() {
        OptionMapping diffOpt = mock(OptionMapping.class);
        when(diffOpt.getAsString()).thenReturn("INVALID");
        when(event.getOption("difficulty")).thenReturn(diffOpt);

        handler.handle(event);
        verify(event).reply(cardContaining("Choose Easy, Medium, or Hard."));
        verify(event, never()).deferReply(anyBoolean());
    }
}

