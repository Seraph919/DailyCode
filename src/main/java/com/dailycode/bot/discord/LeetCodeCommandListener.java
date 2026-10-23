package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@Slf4j
@RequiredArgsConstructor
public class LeetCodeCommandListener extends ListenerAdapter {

    private final String testGuildId;

    @Override
    public void onReady(ReadyEvent event) {
        try {
            OptionData username = new OptionData(OptionType.STRING, "username", "Your LeetCode username", true)
                    .setMinLength(1).setMaxLength(100);
            SubcommandData connect = new SubcommandData("connect", "Start, recover or renew your LeetCode account link")
                    .addOptions(username);
            SubcommandData verify = new SubcommandData("verify", "Verify your pending LeetCode account link");
            DiscordCommandRegistration.register(event.getJDA(), Commands.slash("leetcode", "Manage your LeetCode account link")
                    .addSubcommands(connect, verify), testGuildId);
        } catch (RuntimeException failure) {
            log.error("Could not prepare /leetcode registration.", failure);
        }
    }

}
