package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class PingCommandListener extends ListenerAdapter {

    private final String testGuildId;

    // establishing the connection with the guild..
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

        CommandCreateAction registration = guild.upsertCommand("ping", "Check whether DailyCode is responding.");
        RegistrationSuccessLogger onSuccess = new RegistrationSuccessLogger();
        RegistrationFailureLogger onFailure = new RegistrationFailureLogger();
        registration.queue(onSuccess, onFailure);
    }

    private class RegistrationSuccessLogger implements Consumer<Command> {

        @Override
        public void accept(Command command) {
            log.info("Registered /ping in guild {} (command id={}).", testGuildId, command.getId());
        }
    }

    private class RegistrationFailureLogger implements Consumer<Throwable> {

        @Override
        public void accept(Throwable failure) {
            log.error("Could not register /ping in guild {}: {}", testGuildId, failure.getMessage());
        }
    }

}
