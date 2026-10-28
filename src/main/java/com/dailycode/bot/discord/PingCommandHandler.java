package com.dailycode.bot.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class PingCommandHandler implements SlashCommandHandler {

    @Override
    public String name() {
        return "ping";
    }

    @Override
    public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
        return event.reply(CommandCards.create("Pong!", "DailyCode is online."));
    }
}
