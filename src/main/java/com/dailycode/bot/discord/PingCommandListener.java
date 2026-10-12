package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
@RequiredArgsConstructor
public class PingCommandListener extends ListenerAdapter {

    private final String testGuildId;

    @Override
    public void onReady(ReadyEvent event) {
        if (testGuildId.isBlank()) {
            log.warn("Skipping /ping registration: set DISCORD_TEST_GUILD_ID to your test server ID.");
            return;
        }

        Guild guild;
        try {
            guild = event.getJDA().getGuildById(testGuildId);
        } catch (IllegalArgumentException exception) {
            log.error("Skipping /ping registration: DISCORD_TEST_GUILD_ID must be a numeric Discord server ID.");
            return;
        }
        if (guild == null) {
            log.error("Cannot register /ping: guild {} is unavailable. Check the server ID and bot installation.",
                    testGuildId);
            return;
        }

        guild.upsertCommand("ping", "Check whether DailyCode is responding.")
                .queue(command -> log.info("Registered /ping in guild {} (command id={}).",
                                testGuildId, command.getId()),
                        failure -> log.error("Could not register /ping in guild {}: {}",
                                testGuildId, failure.getMessage()));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!"ping".equals(event.getName()) || !event.isFromGuild()
                || !testGuildId.equals(event.getGuild().getId())) {
            return;
        }

        event.reply("Pong!").queue(
                hook -> log.debug("Replied to /ping in guild {}.", testGuildId),
                failure -> log.warn("Could not reply to /ping in guild {}: {}",
                        testGuildId, failure.getMessage()));
    }
}
