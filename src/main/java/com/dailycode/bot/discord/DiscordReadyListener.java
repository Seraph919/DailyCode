package com.dailycode.bot.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Slf4j
public class DiscordReadyListener extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        var bot = event.getJDA().getSelfUser();
        log.info("Discord bot ready: {} (id={}).", bot.getName(), bot.getId());
    }
}
