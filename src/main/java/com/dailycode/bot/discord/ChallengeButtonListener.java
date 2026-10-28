package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class ChallengeButtonListener extends ListenerAdapter {
    private static final String PREFIX = "dailycode:challenge:";
    private static final Pattern BUTTON = Pattern.compile(PREFIX + "(join|status):([1-9][0-9]{0,18})");
    private final ChallengeCommandHandler handler;

    static String id(String action, long challengeId) {
        return PREFIX + action + ":" + challengeId;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith(PREFIX) || event.isAcknowledged()) return;
        var match = BUTTON.matcher(event.getComponentId());
        long challengeId;
        try {
            if (!match.matches()) throw new IllegalArgumentException();
            challengeId = Long.parseLong(match.group(2));
        } catch (IllegalArgumentException failure) {
            event.reply(CommandCards.create("Invalid button", "This challenge button is invalid. Use /challenge instead.")).setEphemeral(true)
                    .queue(null, failureToReply -> log.debug("Could not acknowledge invalid challenge button."));
            return;
        }
        if (!event.isFromGuild() || event.getMember() == null) {
            event.reply(CommandCards.create("Server required", "Use challenge buttons inside a server.")).setEphemeral(true)
                    .queue(null, failure -> log.debug("Could not acknowledge challenge button outside a server."));
            return;
        }
        String action = match.group(1);
        String guild = event.getGuild().getId();
        String user = event.getUser().getId();
        String username = event.getUser().getName();
        // Acknowledge before queueing any database work. IDs survive application restarts.
        event.deferReply(true).queue(
                hook -> handler.afterButtonAcknowledgement(action, challengeId, guild, user, username, hook),
                failure -> log.debug("Could not acknowledge challenge button ({}).", failure.getClass().getSimpleName()));
    }
}
