package com.dailycode.bot.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public interface SlashCommandHandler {

    String name();

    // Prepare a quick, initial reply. The router owns queue() and error handling.
    ReplyCallbackAction handle(SlashCommandInteractionEvent event);
}
