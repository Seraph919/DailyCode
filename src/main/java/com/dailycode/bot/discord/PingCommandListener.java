package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

@RequiredArgsConstructor
public class PingCommandListener extends ListenerAdapter {
    private final String testGuildId;

    @Override
    public void onReady(ReadyEvent event) {
        DiscordCommandRegistration.register(event.getJDA(),
                Commands.slash("ping", "Check whether DailyCode is responding."), testGuildId);
    }
}
