package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class LeetCodeCommandListener extends ListenerAdapter {

    private final String testGuildId;

    @Override
    public void onReady(ReadyEvent event) {
        if (testGuildId.isBlank()) {
            log.warn("Skipping /leetcode registration: set DISCORD_TEST_GUILD_ID.");
            return;
        }
        try {
            Guild guild = event.getJDA().getGuildById(testGuildId);
            if (guild == null) {
                log.warn("Cannot register /leetcode: test guild is unavailable.");
                return;
            }
            OptionData username = new OptionData(OptionType.STRING, "username", "Your LeetCode username", true)
                    .setMinLength(1).setMaxLength(100);
            SubcommandData connect = new SubcommandData("connect", "Start linking your LeetCode account")
                    .addOptions(username);
            SubcommandData verify = new SubcommandData("verify", "Verify your pending LeetCode account link");
            guild.upsertCommand(Commands.slash("leetcode", "Manage your LeetCode account link")
                    .addSubcommands(connect, verify)).queue(new RegistrationSuccess(), new RegistrationFailure());
        } catch (RuntimeException failure) {
            log.error("Could not prepare /leetcode registration.", failure);
        }
    }

    private class RegistrationSuccess implements Consumer<Command> {
        @Override
        public void accept(Command command) {
            log.info("Registered /leetcode connect and /leetcode verify in guild {}.", testGuildId);
        }
    }

    private class RegistrationFailure implements Consumer<Throwable> {
        @Override
        public void accept(Throwable failure) {
            log.warn("Could not register /leetcode ({}).", failure.getClass().getSimpleName());
        }
    }
}
