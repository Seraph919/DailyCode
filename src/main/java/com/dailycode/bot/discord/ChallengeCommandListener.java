package com.dailycode.bot.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ChallengeCommandListener extends ListenerAdapter {

    private final String testGuildId;

    @Override
    public void onReady(ReadyEvent event) {
        if (testGuildId.isBlank()) {
            log.warn("Skipping /challenge registration: set DISCORD_TEST_GUILD_ID.");
            return;
        }
        try {
            Guild guild = event.getJDA().getGuildById(testGuildId);
            if (guild == null) {
                log.warn("Cannot register /challenge: test guild is unavailable.");
                return;
            }
            SubcommandData create = new SubcommandData("create", "Create a coding challenge")
                    .addOptions(
                            new OptionData(OptionType.STRING, "name", "Challenge name", true)
                                    .setMinLength(1).setMaxLength(100),
                            new OptionData(OptionType.STRING, "end_date", "Last day, inclusive (YYYY-MM-DD)", true)
                                    .setMinLength(10).setMaxLength(10),
                            new OptionData(OptionType.STRING, "start_date", "First day (YYYY-MM-DD); defaults to today", false)
                                    .setMinLength(10).setMaxLength(10),
                            new OptionData(OptionType.STRING, "timezone", "Timezone; defaults to Africa/Casablanca", false)
                                    .setMinLength(1).setMaxLength(100));
            SubcommandData join = new SubcommandData("join", "Join a coding challenge")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                            .setMinLength(1).setMaxLength(19));
            SubcommandData today = new SubcommandData("today", "View today's assigned problem")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                            .setMinLength(1).setMaxLength(19));
            SubcommandData check = new SubcommandData("check", "Check your accepted submission for a challenge day")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                                    .setMinLength(1).setMaxLength(19),
                            new OptionData(OptionType.STRING, "date", "YYYY-MM-DD; defaults to today in the challenge timezone", false)
                                    .setMinLength(10).setMaxLength(10));
            SubcommandData automate = new SubcommandData("automate", "Enable daily assignments and announcements (Manage Server)")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                                    .setMinLength(1).setMaxLength(19),
                            new OptionData(OptionType.CHANNEL, "channel", "Where to post daily problems", true)
                                    .setChannelTypes(ChannelType.TEXT),
                            new OptionData(OptionType.STRING, "difficulty", "Problem difficulty; defaults to Easy", false)
                                    .addChoice("Easy", "EASY").addChoice("Medium", "MEDIUM").addChoice("Hard", "HARD"));
            SubcommandData stop = new SubcommandData("stop-auto", "Stop automatic assignments and announcements (Manage Server)")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                            .setMinLength(1).setMaxLength(19));
            guild.upsertCommand(Commands.slash("challenge", "Manage coding challenges")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                            .addSubcommands(create, join, today, automate, stop, check))
                    .queue(new RegistrationSuccess(), new RegistrationFailure());
        } catch (RuntimeException failure) {
            log.error("Could not prepare /challenge registration ({}).", failure.getClass().getSimpleName());
        }
    }

    private class RegistrationSuccess implements Consumer<Command> {
        @Override
        public void accept(Command command) {
            log.info("Registered /challenge create, join, today, automate, stop-auto and check in guild {}.", testGuildId);
        }
    }

    private class RegistrationFailure implements Consumer<Throwable> {
        @Override
        public void accept(Throwable failure) {
            log.warn("Could not register /challenge ({}).", failure.getClass().getSimpleName());
        }
    }
}
