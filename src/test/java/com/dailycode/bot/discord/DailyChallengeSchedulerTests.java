package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.application.challenge.dto.AnnouncementDto;
import com.dailycode.bot.application.challenge.dto.AutomationTargetDto;
import com.dailycode.bot.application.challenge.dto.DailyAssignmentDto;
import com.dailycode.bot.application.problem.ProblemCatalogService;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.SelfMember;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DailyChallengeSchedulerTests {
    @Test
    void missingEmbedPermissionDoesNotConsumeTheAnnouncementAttempt() {
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        SelfMember bot = mock(SelfMember.class);
        TextChannel channel = mock(TextChannel.class);
        ChallengeAutomationService automation = mock(ChallengeAutomationService.class);
        when(jda.getStatus()).thenReturn(JDA.Status.CONNECTED);
        when(jda.getGuildById("100")).thenReturn(guild);
        when(guild.getSelfMember()).thenReturn(bot);
        when(guild.getTextChannelById("200")).thenReturn(channel);
        when(channel.canTalk()).thenReturn(true);
        when(automation.targets(0)).thenReturn(List.of(new AutomationTargetDto(1L, "100", "200", ProblemDifficulty.EASY)));

        new DailyChallengeScheduler(jda, automation, Clock.systemUTC(), mock(ProblemCatalogService.class)).tick();

        verify(automation, never()).prepare(anyLong(), anyString(), anyString());
        verify(channel, never()).sendMessage(any(MessageCreateData.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "300")
    void announcementOnlyPingsTheOptionalConfiguredRole(String roleId) {
        JDA jda = mock(JDA.class);
        Guild guild = mock(Guild.class);
        TextChannel channel = mock(TextChannel.class);
        ChallengeAutomationService automation = mock(ChallengeAutomationService.class);
        MessageCreateAction action = mock(MessageCreateAction.class, RETURNS_SELF);
        Message sent = mock(Message.class);
        Instant now = Instant.parse("2026-10-22T10:00:00Z");
        var assignment = new DailyAssignmentDto(10L, 1L, 20L, LocalDate.of(2026, 10, 22), 1,
                "Two Sum @here <@456>", "two-sum", ProblemDifficulty.EASY,
                Instant.parse("2026-10-23T00:00:00Z"), now, true);
        when(jda.getStatus()).thenReturn(JDA.Status.CONNECTED);
        when(jda.getGuildById("100")).thenReturn(guild);
        when(guild.getTextChannelById("200")).thenReturn(channel);
        when(channel.canTalk()).thenReturn(true);
        SelfMember bot = mock(SelfMember.class);
        when(guild.getSelfMember()).thenReturn(bot);
        when(bot.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(automation.targets(0)).thenReturn(List.of(new AutomationTargetDto(1L, "100", "200", ProblemDifficulty.EASY)));
        when(automation.prepare(1L, "100", "200")).thenReturn(Optional.of(
                new AnnouncementDto("Practice @everyone <@&999>", "100", "200", roleId, assignment, "1", 14)));
        when(channel.sendMessage(any(MessageCreateData.class))).thenReturn(action);
        when(action.submit()).thenReturn(CompletableFuture.completedFuture(sent));
        when(sent.getId()).thenReturn("400");

        new DailyChallengeScheduler(jda, automation, Clock.fixed(now, ZoneOffset.UTC),
                mock(ProblemCatalogService.class)).tick();

        ArgumentCaptor<MessageCreateData> payload = ArgumentCaptor.forClass(MessageCreateData.class);
        verify(channel).sendMessage(payload.capture());
        var message = payload.getValue();
        assertThat(message.getAllowedMentions()).isEmpty();
        assertThat(message.getMentionedUsers()).isEmpty();
        if (roleId == null) {
            assertThat(message.getMentionedRoles()).isEmpty();
            assertThat(message.getContent()).startsWith("🚀");
        } else {
            assertThat(message.getMentionedRoles()).containsExactly(roleId);
            assertThat(message.getContent()).startsWith("<@&300>\n🚀");
        }
        assertThat(message.getEmbeds()).hasSize(1);
        var embed = message.getEmbeds().getFirst();
        assertThat(embed.getTitle()).startsWith("#1. Two Sum");
        assertThat(embed.getUrl()).isEqualTo("https://leetcode.com/problems/two-sum/");
        assertThat(embed.getFields()).anySatisfy(field -> {
            assertThat(field.getName()).isEqualTo("Participants");
            assertThat(field.getValue()).isEqualTo("14 coders");
        });
        assertThat(embed.getFields()).anySatisfy(field -> {
            assertThat(field.getName()).isEqualTo("Deadline");
            assertThat(field.getValue()).contains("<t:" + assignment.getDeadline().getEpochSecond() + ":R>");
        });
        assertThat(message.getComponents()).hasSize(1);
        verify(automation).delivered(10L, "400");
    }
}
