package com.dailycode.bot.discord;

import com.dailycode.bot.application.challenge.ChallengeCreationException;
import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.application.challenge.DailyAssignmentException;
import com.dailycode.bot.application.challenge.DailyCompletionService;
import com.dailycode.bot.application.challenge.DailyCompletionException;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.application.challenge.ChallengeJoinException;
import com.dailycode.bot.application.challenge.ChallengeJoinService;
import com.dailycode.bot.application.challenge.ChallengeService;
import com.dailycode.bot.application.challenge.ChallengeTodayService;
import com.dailycode.bot.application.challenge.ChallengeTodayException;
import com.dailycode.bot.application.challenge.dto.ChallengeTodayDto;
import com.dailycode.bot.application.challenge.dto.ChallengeDto;
import com.dailycode.bot.application.challenge.dto.ChallengeParticipantDto;
import com.dailycode.bot.application.user.DiscordUserService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class ChallengeCommandHandler implements SlashCommandHandler {

    private final DiscordUserService users;
    private final ChallengeService challenges;
    private final ChallengeJoinService joins;
    private final ChallengeTodayService today;
    private final ChallengeAutomationService automation;
    private final DailyCompletionService completion;
    private final TaskExecutor executor;

    @Override
    public String name() {
        return "challenge";
    }

    @Override
    public ReplyCallbackAction handle(SlashCommandInteractionEvent event) {
        try {
            if ("join".equals(event.getSubcommandName()) || "today".equals(event.getSubcommandName())) {
                readChallengeId(event);
            } else if ("check".equals(event.getSubcommandName())) {
                readCheckOptions(event);
            } else if ("automate".equals(event.getSubcommandName()) || "stop-auto".equals(event.getSubcommandName())) {
                readAutomationOptions(event);
            } else {
                readOptions(event);
            }
            return event.deferReply(true);
        } catch (InvalidOptionsException failure) {
            return event.reply(failure.getMessage()).setEphemeral(true);
        }
    }

    @Override
    public void afterAcknowledgement(SlashCommandInteractionEvent event, InteractionHook hook) {
        Runnable task;
        try {
            if ("join".equals(event.getSubcommandName())) {
                Long challengeId = readChallengeId(event);
                task = new JoinTask(event.getGuild().getId(), event.getUser().getId(),
                        event.getUser().getName(), challengeId, hook);
            } else if ("today".equals(event.getSubcommandName())) {
                Long challengeId = readChallengeId(event);
                task = new TodayTask(event.getGuild().getId(), challengeId, hook);
            } else if ("check".equals(event.getSubcommandName())) {
                CheckOptions options = readCheckOptions(event);
                task = new CheckTask(event.getGuild().getId(), event.getUser().getId(), options, hook);
            } else if ("automate".equals(event.getSubcommandName()) || "stop-auto".equals(event.getSubcommandName())) {
                AutomationOptions options = readAutomationOptions(event);
                task = new AutomationTask(event.getGuild().getId(), options, hook);
            } else {
                CreateOptions options = readOptions(event);
                task = new CreateTask(event.getGuild().getId(), event.getUser().getId(),
                        event.getUser().getName(), options, hook);
            }
        } catch (InvalidOptionsException failure) {
            return; // The initial reply already explains the rejected request.
        }
        try {
            executor.execute(task);
        } catch (TaskRejectedException failure) {
            updateReply(hook, "The bot is busy. Please try again shortly.");
        }
    }

    private CreateOptions readOptions(SlashCommandInteractionEvent event) {
        requireServerMember(event);
        if (!"create".equals(event.getSubcommandName())) {
            throw new InvalidOptionsException("This challenge command is not available yet.");
        }
        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            throw new InvalidOptionsException("You need Manage Server permission to create a challenge.");
        }
        return new CreateOptions(readText(event, "name", 100),
                event.getOption("start_date") == null ? null : readDate(event, "start_date"),
                readDate(event, "end_date"),
                event.getOption("timezone") == null ? null : readText(event, "timezone", 100));
    }

    private CheckOptions readCheckOptions(SlashCommandInteractionEvent event) {
        Long id = readChallengeId(event);
        LocalDate date = event.getOption("date") == null ? null : readDate(event, "date");
        return new CheckOptions(id, date);
    }

    @RequiredArgsConstructor
    private static class CheckOptions {
        private final Long challengeId;
        private final LocalDate date;
    }

    @RequiredArgsConstructor
    private class CheckTask implements Runnable {
        private final String guildId;
        private final String userId;
        private final CheckOptions options;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                var result = options.date == null
                        ? completion.checkToday(options.challengeId, guildId, userId)
                        : completion.check(options.challengeId, guildId, userId, options.date);
                String day = "challenge #" + result.getChallengeId() + " for " + result.getAssignmentDate();
                message = switch (result.getOutcome()) {
                    case COMPLETED -> "You completed " + day + "!";
                    case ALREADY_COMPLETED -> "You already completed " + day + ".";
                    case NO_MATCH -> "No eligible recent accepted submission found for challenge #"
                            + result.getChallengeId() + " (" + result.getAssignmentDate()
                            + "). If you just submitted, try again shortly.";
                };
            } catch (DailyCompletionException failure) {
                message = switch (failure.getReason()) {
                    case CHALLENGE_NOT_FOUND -> "That challenge was not found in this server.";
                    case CHALLENGE_CANCELLED -> "This challenge has been cancelled.";
                    case DATE_OUTSIDE_CHALLENGE -> "That day is outside the challenge dates.";
                    case DAY_NOT_STARTED -> "That challenge day has not started yet.";
                    case NOT_JOINED -> "Join this challenge before checking a submission.";
                    case ACCOUNT_NOT_VERIFIED -> "Verify your LeetCode account before checking a submission.";
                    case NO_ASSIGNMENT -> "No problem has been assigned for that day yet.";
                    case NOT_ELIGIBLE_FOR_DAY -> "You weren't eligible to participate before that day's deadline.";
                    case STATE_CHANGED -> "Your account or challenge changed during the check. Please try again.";
                };
            } catch (LeetCodeClientException failure) {
                log.warn("Could not fetch submissions for a challenge check ({}).", failure.getClass().getSimpleName());
                message = "Couldn't check LeetCode right now. Please try again shortly.";
            } catch (ConstraintViolationException failure) {
                message = "Check the challenge ID and date, then try again.";
            } catch (RuntimeException failure) {
                log.error("Could not check challenge completion ({}).", failure.getClass().getSimpleName());
                message = "Could not check your completion. Please try again later.";
            }
            updateReply(hook, message);
        }
    }

    private AutomationOptions readAutomationOptions(SlashCommandInteractionEvent event) {
        requireServerMember(event);
        if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            throw new InvalidOptionsException("You need Manage Server permission to change automation.");
        }
        Long id = readChallengeId(event);
        if ("stop-auto".equals(event.getSubcommandName())) {
            return new AutomationOptions(id, null, null);
        }
        OptionMapping option = event.getOption("channel");
        if (option == null || option.getAsChannel().getType() != ChannelType.TEXT) {
            throw new InvalidOptionsException("Choose a text channel in this server.");
        }
        var channel = option.getAsChannel().asTextChannel();
        if (!channel.getGuild().getId().equals(event.getGuild().getId())) {
            throw new InvalidOptionsException("Choose a text channel in this server.");
        }
        if (!channel.canTalk()) {
            throw new InvalidOptionsException("I need View Channel and Send Messages permissions in that channel.");
        }
        ProblemDifficulty difficulty = ProblemDifficulty.EASY;
        if (event.getOption("difficulty") != null) {
            try {
                difficulty = ProblemDifficulty.valueOf(readText(event, "difficulty", 10));
            } catch (IllegalArgumentException failure) {
                throw new InvalidOptionsException("Choose Easy, Medium, or Hard.");
            }
        }
        return new AutomationOptions(id, channel.getId(), difficulty);
    }

    private void requireServerMember(SlashCommandInteractionEvent event) {
        if (!event.isFromGuild() || event.getGuild() == null) {
            throw new InvalidOptionsException("Use this command in a server.");
        }
        if (event.getMember() == null) {
            throw new InvalidOptionsException("Could not identify your server membership. Please try again.");
        }
    }

    private Long readChallengeId(SlashCommandInteractionEvent event) {
        requireServerMember(event);
        String value = readText(event, "id", 19);
        if (value.matches("[0-9]+")) {
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException failure) {
                // The value is larger than a database BIGINT.
            }
        }
        throw new InvalidOptionsException("Enter a valid positive challenge ID.");
    }

    private String readText(SlashCommandInteractionEvent event, String name, int maxLength) {
        OptionMapping option = event.getOption(name);
        if (option == null) {
            throw new InvalidOptionsException("Provide " + name + ".");
        }
        String value = option.getAsString().strip();
        if (value.isBlank() || value.length() > maxLength) {
            throw new InvalidOptionsException("Enter " + name + " containing 1 to " + maxLength + " characters.");
        }
        return value;
    }

    private LocalDate readDate(SlashCommandInteractionEvent event, String name) {
        String value = readText(event, name, 10);
        if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new InvalidOptionsException("Enter " + name + " as YYYY-MM-DD.");
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() == 0) {
                throw new InvalidOptionsException("Enter " + name + " with a year from 0001 to 9999.");
            }
            return date;
        } catch (DateTimeParseException failure) {
            throw new InvalidOptionsException("Enter a valid " + name + " as YYYY-MM-DD.");
        }
    }

    private String creationMessage(ChallengeCreationException failure) {
        return switch (failure.getReason()) {
            case INVALID_TIMEZONE -> "Enter a valid timezone, such as Africa/Casablanca.";
            case START_DATE_IN_PAST -> "The start date must be today or later in the challenge's timezone.";
            case END_DATE_BEFORE_START -> "The end date must be on or after the start date.";
            case USER_NOT_REGISTERED -> "Your Discord registration is missing. Please try again.";
            case ACCOUNT_NOT_VERIFIED -> "Verify your LeetCode account before creating a challenge.";
        };
    }

    private String joinMessage(ChallengeJoinException failure) {
        return switch (failure.getReason()) {
            case CHALLENGE_NOT_FOUND -> "That challenge was not found in this server.";
            case ALREADY_JOINED -> "You already joined this challenge.";
            case CHALLENGE_CANCELLED -> "This challenge has been cancelled.";
            case CHALLENGE_COMPLETED -> "This challenge is completed.";
            case CHALLENGE_ENDED -> "This challenge has ended.";
            case USER_NOT_REGISTERED -> "Your Discord registration is missing. Please try again.";
            case ACCOUNT_NOT_VERIFIED -> "Verify your LeetCode account before joining a challenge.";
        };
    }

    private String todayMessage(ChallengeTodayException failure) {
        return switch (failure.getReason()) {
            case CHALLENGE_NOT_FOUND -> "That challenge was not found in this server.";
            case CHALLENGE_CANCELLED -> "This challenge has been cancelled.";
            case CHALLENGE_COMPLETED -> "This challenge is completed.";
            case CHALLENGE_NOT_STARTED -> "This challenge has not started yet.";
            case CHALLENGE_ENDED -> "This challenge has ended.";
            case NO_ASSIGNMENT -> "No problem has been assigned for today yet.";
        };
    }

    @RequiredArgsConstructor
    private class AutomationTask implements Runnable {
        private final String guildId;
        private final AutomationOptions options;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                if (options.channelId == null) {
                    automation.disable(options.challengeId, guildId);
                    message = "Automation is off for challenge #" + options.challengeId + ".";
                } else {
                    automation.enable(options.challengeId, guildId, options.channelId, options.difficulty);
                    message = "Daily assignments and submission checks enabled for challenge #" + options.challengeId + " in <#"
                            + options.channelId + "> (" + options.difficulty + ").";
                }
            } catch (DailyAssignmentException failure) {
                message = switch (failure.getReason()) {
                    case CHALLENGE_NOT_FOUND -> "That challenge was not found in this server.";
                    case CHALLENGE_CANCELLED -> "This challenge has been cancelled.";
                    case CHALLENGE_COMPLETED -> "This challenge is completed.";
                    case CHALLENGE_ENDED -> "This challenge has ended.";
                    default -> "Could not change automation. Please try again later.";
                };
            } catch (ConstraintViolationException failure) {
                message = "Check the challenge ID and channel, then try again.";
            } catch (RuntimeException failure) {
                log.error("Could not change challenge automation ({}).", failure.getClass().getSimpleName());
                message = "Could not change automation. Please try again later.";
            }
            updateReply(hook, message);
        }
    }

    @RequiredArgsConstructor
    private static class AutomationOptions {
        private final Long challengeId;
        private final String channelId;
        private final ProblemDifficulty difficulty;
    }

    @RequiredArgsConstructor
    private class TodayTask implements Runnable {
        private final String guildId;
        private final Long challengeId;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                ChallengeTodayDto result = today.today(challengeId, guildId);
                long deadline = result.getDeadline().getEpochSecond();
                message = "Challenge #" + result.getChallengeId() + ": "
                        + MarkdownSanitizer.escape(result.getChallengeName()) + "\n"
                        + "Day " + result.getDayNumber() + " · " + result.getDate() + " (" + result.getTimezone() + ")\n"
                        + MarkdownSanitizer.escape(result.getTitle()) + " (" + result.getDifficulty() + ")\n"
                        + "https://leetcode.com/problems/" + result.getTitleSlug() + "/\n"
                        + "Deadline: <t:" + deadline + ":F> (<t:" + deadline + ":R>)";
            } catch (ChallengeTodayException failure) {
                message = todayMessage(failure);
            } catch (ConstraintViolationException failure) {
                message = "Check the challenge ID, then try again.";
            } catch (RuntimeException failure) {
                log.error("Could not load today's challenge ({}).", failure.getClass().getSimpleName());
                message = "Could not load today's problem. Please try again later.";
            }
            updateReply(hook, message);
        }
    }

    private void updateReply(InteractionHook hook, String message) {
        try {
            hook.editOriginal(message).setAllowedMentions(List.of())
                    .queue(null, new ReplyFailure());
        } catch (RuntimeException failure) {
            log.warn("Could not queue the challenge response ({}).", failure.getClass().getSimpleName());
        }
    }

    private class ReplyFailure implements Consumer<Throwable> {
        @Override
        public void accept(Throwable failure) {
            log.warn("Could not deliver the challenge response ({}).", failure.getClass().getSimpleName());
        }
    }

    @RequiredArgsConstructor
    private static class CreateOptions {
        private final String name;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final String timezone;
    }

    private static class InvalidOptionsException extends RuntimeException {
        private InvalidOptionsException(String message) {
            super(message);
        }
    }

    @RequiredArgsConstructor
    private class JoinTask implements Runnable {
        private final String guildId;
        private final String userId;
        private final String username;
        private final Long challengeId;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                users.register(userId, username);
                ChallengeParticipantDto participant = joins.join(challengeId, guildId, userId);
                message = "You joined challenge #" + participant.getChallengeId() + ": "
                        + participant.getChallengeName() + ".";
            } catch (ChallengeJoinException failure) {
                message = joinMessage(failure);
            } catch (ConstraintViolationException failure) {
                message = "Check the challenge ID, then try again.";
            } catch (RuntimeException failure) {
                log.error("Could not join a challenge ({}).", failure.getClass().getSimpleName());
                message = "Could not join the challenge. Please try again later.";
            }
            updateReply(hook, message);
        }
    }

    @RequiredArgsConstructor
    private class CreateTask implements Runnable {
        private final String guildId;
        private final String userId;
        private final String username;
        private final CreateOptions options;
        private final InteractionHook hook;

        @Override
        public void run() {
            String message;
            try {
                users.register(userId, username);
                ChallengeDto challenge = challenges.create(guildId, userId, options.name,
                        options.startDate, options.endDate, options.timezone);
                message = "Created challenge #" + challenge.getId() + ": " + challenge.getName() + ".\n"
                        + "Scheduled " + challenge.getStartDate() + " to " + challenge.getEndDate()
                        + " (" + challenge.getTimezone() + ").";
            } catch (ChallengeCreationException failure) {
                message = creationMessage(failure);
            } catch (ConstraintViolationException failure) {
                message = "Check the challenge name, dates and timezone, then try again.";
            } catch (RuntimeException failure) {
                log.error("Could not create a challenge ({}).", failure.getClass().getSimpleName());
                message = "Could not create the challenge. Please try again later.";
            }
            updateReply(hook, message);
        }
    }
}
