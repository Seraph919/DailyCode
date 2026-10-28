package com.dailycode.bot.discord;

import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import static com.dailycode.bot.discord.CardMatchers.*;

import com.dailycode.bot.application.leetcode.LeetCodeLinkService;
import com.dailycode.bot.application.leetcode.LeetCodeVerificationService;
import com.dailycode.bot.application.leetcode.dto.LeetCodeLinkChallengeDto;
import com.dailycode.bot.application.user.DiscordUserService;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LeetCodeCommandHandlerTests {

    private final DiscordUserService users = mock(DiscordUserService.class);
    private final LeetCodeLinkService links = mock(LeetCodeLinkService.class);
    private final LeetCodeVerificationService verification = mock(LeetCodeVerificationService.class);
    private final LeetCodeCommandHandler handler = new LeetCodeCommandHandler(
            users, links, verification, new SyncTaskExecutor());

    private final SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class);
    private final InteractionHook hook = mock(InteractionHook.class);
    private final User user = mock(User.class);
    private final OptionMapping usernameOption = mock(OptionMapping.class);
    private final WebhookMessageEditAction editAction = mock(WebhookMessageEditAction.class, RETURNS_SELF);

    @BeforeEach
    void setUp() {
        when(event.getUser()).thenReturn(user);
        when(user.getId()).thenReturn("123456789012345678");
        when(user.getName()).thenReturn("alice_discord");
        when(event.getOption("username")).thenReturn(usernameOption);
        when(event.deferReply(true)).thenReturn(mock(ReplyCallbackAction.class));
        when(event.reply(any(MessageCreateData.class))).thenReturn(mock(ReplyCallbackAction.class, RETURNS_SELF));
        when(hook.editOriginal(any(MessageEditData.class))).thenReturn(editAction);
    }

    @Test
    void nameIsLeetCode() {
        assertThat(handler.name()).isEqualTo("leetcode");
    }

    @Test
    void handlesConnectAndExecutesLink() {
        when(event.getSubcommandName()).thenReturn("connect");
        when(usernameOption.getAsString()).thenReturn("alice_lc");

        var challenge = new LeetCodeLinkChallengeDto("alice_lc", "dailycode-token123", Instant.now().plusSeconds(600));
        when(links.startLink("123456789012345678", "alice_lc")).thenReturn(challenge);

        handler.handle(event);
        handler.afterAcknowledgement(event, hook);

        verify(users).register("123456789012345678", "alice_discord");
        verify(links).startLink("123456789012345678", "alice_lc");
        verify(hook).editOriginal(editContaining("dailycode-token123"));
    }

    @Test
    void handlesVerifyAndExecutesVerification() {
        when(event.getSubcommandName()).thenReturn("verify");

        handler.handle(event);
        handler.afterAcknowledgement(event, hook);

        verify(verification).verify("123456789012345678");
        verify(hook).editOriginal(editContaining("Your LeetCode account is verified!"));
    }

    @Test
    void rejectsMissingUsernameForConnect() {
        when(event.getSubcommandName()).thenReturn("connect");
        when(event.getOption("username")).thenReturn(null);

        handler.handle(event);

        verify(event).reply(cardContaining("Enter a LeetCode username containing 1 to 100 characters."));
    }
}

