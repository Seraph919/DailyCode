package com.dailycode.bot.discord;

import com.dailycode.bot.application.leetcode.LeetCodeLinkService;
import com.dailycode.bot.application.leetcode.LeetCodeVerificationService;
import com.dailycode.bot.application.leetcode.dto.LeetCodeLinkChallengeDto;
import com.dailycode.bot.application.user.DiscordUserService;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class LeetCodeCommandHandler implements SlashCommandHandler {

    private final DiscordUserService users;
    private final LeetCodeLinkService links;
    private final LeetCodeVerificationService verification;
    private final TaskExecutor executor;

    @Override
    public String name() {
        return "leetcode";
    }

    @Override
    public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
        if ("verify".equals(event.getSubcommandName())) {
            return event.deferReply(true);
        }
        if (!"connect".equals(event.getSubcommandName())) {
            return event.reply("This LeetCode command is not available yet.").setEphemeral(true);
        }
        if (readUsername(event) == null) {
            return event.reply("Enter a LeetCode username containing 1 to 100 characters.").setEphemeral(true);
        }
        return event.deferReply(true);
    }

    @Override
    public void afterAcknowledgement(SlashCommandInteractionEvent event, InteractionHook hook) {
        if ("verify".equals(event.getSubcommandName())) {
            submit(new VerifyTask(event.getUser().getId(), hook), hook);
            return;
        }
        if (!"connect".equals(event.getSubcommandName())) {
            return;
        }
        String username = readUsername(event);
        if (username == null) {
            return;
        }
        ConnectTask task = new ConnectTask(event.getUser().getId(), event.getUser().getName(), username, hook);
        submit(task, hook);
    }

    private void submit(Runnable task, InteractionHook hook) {
        try {
            executor.execute(task);
        } catch (TaskRejectedException failure) {
            updateReply(hook, "The bot is busy. Please try again shortly.");
        }
    }

    private String readUsername(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("username");
        if (option == null) {
            return null;
        }
        String username = option.getAsString().strip();
        if (username.isBlank() || username.length() > 100) {
            return null;
        }
        return username;
    }

    private void updateReply(InteractionHook hook, String message) {
        try {
            hook.editOriginal(message).setAllowedMentions(List.of()).queue(null, new ReplyFailure());
        } catch (RuntimeException failure) {
            log.warn("Could not queue the private link response ({}).", failure.getClass().getSimpleName());
        }
    }

    private class ReplyFailure implements Consumer<Throwable> {
        @Override
        public void accept(Throwable failure) {
            log.warn("Could not deliver the private link response ({}).", failure.getClass().getSimpleName());
        }
    }

    @RequiredArgsConstructor
    private class ConnectTask implements Runnable {
        private final String discordUserId;
        private final String discordUsername;
        private final String leetcodeUsername;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                users.register(discordUserId, discordUsername);
                LeetCodeLinkChallengeDto challenge = links.startLink(discordUserId, leetcodeUsername);
                message = "Verification challenge ready for " + challenge.getLeetcodeUsername()
                        + ". In your LeetCode profile's ReadMe, add this token on its own line:\n"
                        + "```\n" + challenge.getVerificationToken() + "\n```\n"
                        + "Keep your existing profile text. This challenge expires <t:"
                        + challenge.getExpiresAt().getEpochSecond() + ":R>.\n"
                        + "Save your ReadMe, then run /leetcode verify before the deadline. Copy only the token, without the code-block formatting. "
                        + "Run /leetcode connect with the same username to recover this token or renew it after expiry. "
                        + "If renewed, replace the old token in ReadMe.";
            } catch (LeetCodeClientException failure) {
                message = "Could not read that LeetCode profile. Check the username and try again later.";
            } catch (DataIntegrityViolationException failure) {
                message = "Could not prepare the challenge because of a database conflict. An account link may already exist. "
                        + "Retry /leetcode connect with the same username.";
            } catch (IllegalStateException failure) {
                message = "Could not prepare a challenge. Use the same LeetCode username as your pending link. "
                        + "Verified accounts cannot be linked again, and usernames reserved by another Discord user cannot be claimed.";
            } catch (RuntimeException failure) {
                // Exception details can contain SQL parameters, including challenge tokens.
                log.error("Could not start a LeetCode link ({}).", failure.getClass().getSimpleName());
                message = "Could not start the link. Please try again later.";
            }
            updateReply(hook, message);
        }
    }

    @RequiredArgsConstructor
    private class VerifyTask implements Runnable {
        private final String discordUserId;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                verification.verify(discordUserId);
                message = "Your LeetCode account is verified! You can now remove the verification token from your ReadMe.";
            } catch (LeetCodeClientException failure) {
                message = "Could not read your LeetCode profile. Please try again later while your challenge is still active.";
            } catch (IllegalStateException failure) {
                message = "Verification did not complete. You need a pending, unexpired link and its exact token on its own line in ReadMe. "
                        + "Save the profile before trying again. To recover or renew a pending challenge, run /leetcode connect with the same username. "
                        + "Already-verified accounts do not need verification again.";
            } catch (RuntimeException failure) {
                log.error("Could not verify a LeetCode link ({}).", failure.getClass().getSimpleName());
                message = "Could not complete verification. Please try again later.";
            }
            updateReply(hook, message);
        }
    }
}
