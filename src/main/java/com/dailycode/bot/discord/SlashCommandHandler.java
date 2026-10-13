package com.dailycode.bot.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public interface SlashCommandHandler {

    String name();

    // Prepare a quick, initial reply. The router owns queue() and error handling.
    ReplyCallbackAction handle(SlashCommandInteractionEvent event);

    // Called only after Discord accepts the initial reply or deferred reply.
    // Slow handlers should submit work to an executor here, not block this callback.
    default void afterAcknowledgement(SlashCommandInteractionEvent event, InteractionHook hook) {
        // Quick handlers such as /ping have nothing more to do.
    }
}
