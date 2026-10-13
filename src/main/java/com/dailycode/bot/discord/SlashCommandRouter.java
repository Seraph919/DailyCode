package com.dailycode.bot.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class SlashCommandRouter extends ListenerAdapter {

    private final String testGuildId;
    private final Map<String, SlashCommandHandler> handlers;

    public SlashCommandRouter(String testGuildId, List<SlashCommandHandler> commandHandlers)
    {
        this.testGuildId = testGuildId;
        Map<String, SlashCommandHandler> registered = new HashMap<>();
        for (SlashCommandHandler handler : commandHandlers) {
            String commandName = handler.name();
            if (registered.containsKey(commandName)) {
                throw new IllegalStateException("Duplicate slash command handler: " + commandName);
            }
            registered.put(commandName, handler);
        }
        this.handlers = Map.copyOf(registered);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        if (!testGuildId.equals(event.getGuild().getId())) {
            return;
        }
        if (event.isAcknowledged()) {
            return;
        }

        ReplyCallbackAction reply;
        SlashCommandHandler handler = null;
        try {
            handler = handlers.get(event.getName());
            if (handler == null) {
                reply = event.reply("This command is not available yet.");
                reply.setEphemeral(true);
            } else {
                reply = handler.handle(event);
                if (reply == null) {
                    throw new IllegalStateException("Handler must return a reply");
                }
            }
        } catch (RuntimeException failure) {
            handler = null;
            log.error("Failed to handle /{}.", event.getName(), failure);
            // A handler must not send replies itself. Avoid a second acknowledgement if one did.
            if (event.isAcknowledged()) {
                return;
            }
            try {
                reply = event.reply("Something went wrong. Please try again.");
                reply.setEphemeral(true);
            } catch (RuntimeException replyFailure) {
                log.error("Could not prepare the error reply for /{}.", event.getName(), replyFailure);
                return;
            }
        }
        sendReply(event, reply, handler);
    }

    private void sendReply(SlashCommandInteractionEvent event, ReplyCallbackAction reply,
                           SlashCommandHandler handler) {
        try {
            String commandName = event.getName();
            ReplySuccessCallback onSuccess = new ReplySuccessCallback(event, handler);
            ReplyFailureLogger onFailure = new ReplyFailureLogger(commandName);
            reply.queue(onSuccess, onFailure);
        } catch (RuntimeException failure) {
            log.warn("Could not queue reply for /{}.", event.getName(), failure);
        }
    }

    private class ReplySuccessCallback implements Consumer<InteractionHook> {

        private final SlashCommandInteractionEvent event;
        private final SlashCommandHandler handler;

        private ReplySuccessCallback(SlashCommandInteractionEvent event, SlashCommandHandler handler) {
            this.event = event;
            this.handler = handler;
        }

        @Override
        public void accept(InteractionHook hook) {
            String commandName = event.getName();
            log.debug("Replied to /{} in guild {}.", commandName, testGuildId);
            if (handler == null) {
                return;
            }
            try {
                handler.afterAcknowledgement(event, hook);
            } catch (RuntimeException failure) {
                log.error("Could not start work for /{} after acknowledgement.", commandName, failure);
                try {
                    hook.editOriginal("Something went wrong. Please try again.")
                            .queue(null, new ReplyFailureLogger(commandName));
                } catch (RuntimeException replyFailure) {
                    log.warn("Could not queue the error update for /{}.", commandName, replyFailure);
                }
            }
        }
    }

    private class ReplyFailureLogger implements Consumer<Throwable> {

        private final String commandName;

        private ReplyFailureLogger(String commandName) {
            this.commandName = commandName;
        }

        @Override
        public void accept(Throwable failure) {
            log.warn("Could not deliver reply for /{}.", commandName, failure);
        }
    }
}
