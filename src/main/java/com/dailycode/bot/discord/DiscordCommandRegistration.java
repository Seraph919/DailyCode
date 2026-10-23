package com.dailycode.bot.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slf4j
final class DiscordCommandRegistration {
    private DiscordCommandRegistration() {
    }

    static void register(JDA jda, CommandData command, String testGuildId) {
        command.setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
        try {
            jda.upsertCommand(command).queue(
                    saved -> log.info("Registered /{} globally.", command.getName()),
                    failure -> log.warn("Could not register /{} globally ({}).",
                            command.getName(), failure.getClass().getSimpleName()));
        } catch (RuntimeException failure) {
            log.warn("Could not queue global /{} registration ({}).",
                    command.getName(), failure.getClass().getSimpleName());
        }

        // Optional immediate development updates, also refreshing legacy guild commands.
        // This setting never limits global registration or interaction routing.
        if (testGuildId.isBlank()) return;
        try {
            var guild = jda.getGuildById(testGuildId);
            if (guild == null) {
                log.warn("Cannot mirror /{}: test guild {} is unavailable.", command.getName(), testGuildId);
                return;
            }
            guild.upsertCommand(command).queue(
                    saved -> log.info("Mirrored /{} in test guild {}.", command.getName(), testGuildId),
                    failure -> log.warn("Could not mirror /{} in test guild {} ({}).",
                            command.getName(), testGuildId, failure.getClass().getSimpleName()));
        } catch (RuntimeException failure) {
            log.warn("Could not mirror /{}: check DISCORD_TEST_GUILD_ID ({}).",
                    command.getName(), failure.getClass().getSimpleName());
        }
    }
}
