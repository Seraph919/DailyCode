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
            SubcommandData status = new SubcommandData("status", "View your saved challenge progress and history")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                                    .setMinLength(1).setMaxLength(19),
                            new OptionData(OptionType.INTEGER, "page", "History page; newest first", false)
                                    .setMinValue(1).setMaxValue(1000000));
            SubcommandData check = new SubcommandData("check", "Check your accepted submission for a challenge day")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                                    .setMinLength(1).setMaxLength(19),
                            new OptionData(OptionType.STRING, "date", "YYYY-MM-DD; defaults to today in the challenge timezone", false)
                                    .setMinLength(10).setMaxLength(10));
            SubcommandData automate = new SubcommandData("automate", "Enable daily problems and submission checks (Manage Server)")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                                    .setMinLength(1).setMaxLength(19),
                            new OptionData(OptionType.CHANNEL, "channel", "Where to post daily problems", true)
                                    .setChannelTypes(ChannelType.TEXT),
                            new OptionData(OptionType.STRING, "difficulty", "Problem difficulty; defaults to Easy", false)
                                    .addChoice("Easy", "EASY").addChoice("Medium", "MEDIUM").addChoice("Hard", "HARD"),
                            new OptionData(OptionType.STRING, "time", "Daily posting time HH:mm (24-hour), challenge timezone; defaults to 00:00", false)
                                    .setMinLength(5).setMaxLength(5),
                            new OptionData(OptionType.ROLE, "role", "Role to mention in daily posts; omit for no mention", false));
            SubcommandData stop = new SubcommandData("stop-auto", "Stop daily problems and submission checks (Manage Server)")
                    .addOptions(new OptionData(OptionType.STRING, "id", "Challenge ID", true)
                            .setMinLength(1).setMaxLength(19));
            guild.upsertCommand(Commands.slash("challenge", "Manage coding challenges")
                            .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                            .addSubcommands(create, join, today, automate, stop, check, status))
                    .queue(new RegistrationSuccess(), new RegistrationFailure());
        } catch (RuntimeException failure) {
            log.error("Could not prepare /challenge registration ({}).", failure.getClass().getSimpleName());
        }
    }

    private class RegistrationSuccess implements Consumer<Command> {
        @Override
        public void accept(Command command) {
            log.info("Registered /challenge create, join, today, automate, stop-auto, check and status in guild {}.", testGuildId);
        }
    }

    private class RegistrationFailure implements Consumer<Throwable> {
        @Override
        public void accept(Throwable failure) {
            log.warn("Could not register /challenge ({}).", failure.getClass().getSimpleName());
        }
    }
}
