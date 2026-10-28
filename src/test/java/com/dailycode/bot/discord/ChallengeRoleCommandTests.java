package com.dailycode.bot.discord;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import static com.dailycode.bot.discord.CardMatchers.*;

import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.SelfMember;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.LocalTime;

import static org.mockito.Mockito.*;

class ChallengeRoleCommandTests {
    private final ChallengeAutomationService automation = mock(ChallengeAutomationService.class);
    private final ChallengeCommandHandler handler = new ChallengeCommandHandler(
            null, null, null, null, automation, null, null, null, new SyncTaskExecutor());
    private final SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    private final Guild guild = mock(Guild.class);
    private final SelfMember bot = mock(SelfMember.class);
    private final TextChannel channel = mock(TextChannel.class);
    private final Role role = mock(Role.class);

    @BeforeEach
    void configureCommand() {
        when(event.getSubcommandName()).thenReturn("automate");
        when(event.isFromGuild()).thenReturn(true);
        when(event.getGuild()).thenReturn(guild);
        when(guild.getId()).thenReturn("100");
        when(guild.getSelfMember()).thenReturn(bot);
        Member admin = mock(Member.class);
        when(event.getMember()).thenReturn(admin);
        when(admin.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);
        OptionMapping id = mock(OptionMapping.class);
        when(event.getOption("id")).thenReturn(id);
        when(id.getAsString()).thenReturn("1");
        OptionMapping channelOption = mock(OptionMapping.class, RETURNS_DEEP_STUBS);
        when(event.getOption("channel")).thenReturn(channelOption);
        when(channelOption.getAsChannel().getType()).thenReturn(ChannelType.TEXT);
        when(channelOption.getAsChannel().asTextChannel()).thenReturn(channel);
        when(channel.getGuild()).thenReturn(guild);
        when(channel.getId()).thenReturn("200");
        when(channel.canTalk()).thenReturn(true);
        when(bot.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(true);
        when(role.getGuild()).thenReturn(guild);
        when(role.getId()).thenReturn("300");
        when(role.isMentionable()).thenReturn(true);
        when(event.deferReply(true)).thenReturn(mock(ReplyCallbackAction.class));
        when(event.reply(any(MessageCreateData.class))).thenReturn(mock(ReplyCallbackAction.class, RETURNS_SELF));
    }

    @Test
    void optionalRoleIsPassedToAutomation() {
        selectRole();
        acceptAndRun();
        verify(automation).enable(1L, "100", "200", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, "300");
    }

    @Test
    void omittingRoleClearsTheConfiguredMention() {
        acceptAndRun();
        verify(automation).enable(1L, "100", "200", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, null);
    }

    @Test
    void rejectsEveryone() {
        selectRole();
        when(role.isPublicRole()).thenReturn(true);
        handler.handle(event);
        verify(event).reply(cardContaining("Choose a role in this server other than @everyone."));
        verify(event, never()).deferReply(anyBoolean());
    }

    @Test
    void rejectsRoleFromAnotherServer() {
        selectRole();
        Guild other = mock(Guild.class);
        when(other.getId()).thenReturn("999");
        when(role.getGuild()).thenReturn(other);
        handler.handle(event);
        verify(event).reply(cardContaining("Choose a role in this server other than @everyone."));
        verify(event, never()).deferReply(anyBoolean());
    }

    @Test
    void rejectsRoleTheBotCannotMention() {
        selectRole();
        when(role.isMentionable()).thenReturn(false);
        handler.handle(event);
        verify(event).reply(cardContaining("Make that role mentionable or give me Mention Everyone permission in that channel."));
        verify(event, never()).deferReply(anyBoolean());
    }

    @Test
    void channelPermissionAllowsAnOtherwiseUnmentionableRole() {
        selectRole();
        when(role.isMentionable()).thenReturn(false);
        when(bot.hasPermission(channel, Permission.MESSAGE_MENTION_EVERYONE)).thenReturn(true);
        acceptAndRun();
        verify(automation).enable(1L, "100", "200", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, "300");
    }

    @Test
    void rejectsChannelWithoutEmbedPermission() {
        when(bot.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)).thenReturn(false);
        handler.handle(event);
        verify(event).reply(cardContaining("I need Embed Links permission in that channel to post challenge cards."));
        verify(event, never()).deferReply(anyBoolean());
    }

    private void selectRole() {
        OptionMapping roleOption = mock(OptionMapping.class);
        when(event.getOption("role")).thenReturn(roleOption);
        when(roleOption.getAsRole()).thenReturn(role);
    }

    private void acceptAndRun() {
        handler.handle(event);
        verify(event).deferReply(true);
        handler.afterAcknowledgement(event, mock(InteractionHook.class, RETURNS_DEEP_STUBS));
    }
}
