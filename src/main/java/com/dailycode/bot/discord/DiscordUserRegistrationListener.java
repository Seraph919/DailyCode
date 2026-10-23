package com.dailycode.bot.discord;

import com.dailycode.bot.application.user.DiscordUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

@Slf4j
@RequiredArgsConstructor
public class DiscordUserRegistrationListener extends ListenerAdapter {

    private final DiscordUserService userService;
    private final TaskExecutor registrationExecutor;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        User user = event.getUser();
        if (user.isBot()) {
            return;
        }

        RegistrationTask task = new RegistrationTask(user.getId(), user.getName());
        try {
            registrationExecutor.execute(task);
        } catch (TaskRejectedException failure) {
            log.warn("Could not queue registration for Discord user {}. Try another command.", user.getId());
        }
    }

    private class RegistrationTask implements Runnable {

        private final String discordUserId;
        private final String discordUsername;

        private RegistrationTask(String discordUserId, String discordUsername) {
            this.discordUserId = discordUserId;
            this.discordUsername = discordUsername;
        }

        @Override
        public void run() {
            try {
                userService.register(discordUserId, discordUsername);
                log.info("Saved Discord user {} from a slash command.", discordUserId);
            } catch (RuntimeException failure) {
                log.error("Could not save Discord user {}. Try another command.", discordUserId, failure);
            }
        }
    }
}
