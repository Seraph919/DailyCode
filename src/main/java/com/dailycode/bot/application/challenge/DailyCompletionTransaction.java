package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.challenge.dto.DailyCompletionResultDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.domain.challenge.ChallengeParticipant;
import com.dailycode.bot.domain.challenge.ChallengeStatus;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.challenge.DailyProgress;
import com.dailycode.bot.domain.challenge.DailyProgressStatus;
import com.dailycode.bot.repository.ChallengeParticipantRepository;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.repository.DailyChallengeRepository;
import com.dailycode.bot.repository.DailyProgressRepository;
import com.dailycode.bot.repository.LeetCodeAccountRepository;
import com.dailycode.bot.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

import static com.dailycode.bot.application.challenge.DailyCompletionException.Reason.*;

@Component
@RequiredArgsConstructor
public class DailyCompletionTransaction {
    private final ChallengeRepository challenges;
    private final ChallengeParticipantRepository participants;
    private final DailyChallengeRepository assignments;
    private final LeetCodeAccountRepository accounts;
    private final ProblemRepository problems;
    private final DailyProgressRepository progress;
    private final SubmissionEligibilityChecker eligibility;
    private final Clock clock;

    @Transactional
    public DailyCompletionCheckDto prepare(Long challengeId, String guildId, String discordUserId, LocalDate date) {
        return load(challengeId, guildId, discordUserId, date).check;
    }

    @Transactional
    public DailyCompletionResultDto complete(DailyCompletionCheckDto expected, LeetCodeRecentSubmissionsDto feed) {
        State current = load(expected.getChallengeId(), expected.getGuildId(),
                expected.getDiscordUserId(), expected.getAssignmentDate());
        if (!sameTarget(expected, current.check)) {
            throw new DailyCompletionException(STATE_CHANGED);
        }
        if (current.check.getExistingCompletion() != null) {
            return current.check.getExistingCompletion();
        }
        Instant now = clock.instant();
        var accepted = eligibility.findEligible(current.check, feed, now);
        DailyProgress record = current.progress == null
                ? new DailyProgress(current.participant, current.assignment) : current.progress;
        if (accepted.isPresent()) {
            var submission = accepted.get();
            record.complete(submission.getSubmissionId(), submission.getSubmittedAt(), now);
        }
        // No match only records PENDING; a bounded recent feed cannot establish failure.
        progress.saveAndFlush(record);
        return result(record, current.assignment, accepted.isPresent()
                ? DailyCompletionOutcome.COMPLETED : DailyCompletionOutcome.NO_MATCH);
    }

    private State load(Long challengeId, String guildId, String discordUserId, LocalDate date) {
        // All completion writers serialize on the challenge, including first progress inserts.
        // Lock order: challenge -> membership -> account -> problem. No HTTP in this transaction.
        var challenge = challenges.findForAssignment(challengeId, guildId)
                .orElseThrow(() -> new DailyCompletionException(CHALLENGE_NOT_FOUND));
        if (challenge.getStatus() == ChallengeStatus.CANCELLED) {
            throw new DailyCompletionException(CHALLENGE_CANCELLED);
        }
        ZoneId zone = ZoneId.of(challenge.getTimezone());
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate requestedDate = date == null ? today : date;
        if (requestedDate.isBefore(challenge.getStartDate()) || requestedDate.isAfter(challenge.getEndDate())) {
            throw new DailyCompletionException(DATE_OUTSIDE_CHALLENGE);
        }
        if (requestedDate.isAfter(today)) {
            throw new DailyCompletionException(DAY_NOT_STARTED);
        }
        var participant = participants.findForCompletion(challengeId, discordUserId)
                .orElseThrow(() -> new DailyCompletionException(NOT_JOINED));
        var account = accounts.findForVerificationByDiscordUserId(discordUserId)
                .orElseThrow(() -> new DailyCompletionException(ACCOUNT_NOT_VERIFIED));
        if (!account.isVerified() || account.getVerifiedAt() == null) {
            throw new DailyCompletionException(ACCOUNT_NOT_VERIFIED);
        }
        var assignment = assignments.findByChallenge_IdAndChallenge_DiscordGuildIdAndAssignmentDate(
                        challengeId, guildId, requestedDate)
                .orElseThrow(() -> new DailyCompletionException(NO_ASSIGNMENT));
        var problem = problems.findForCompletion(assignment.getProblem().getId())
                .orElseThrow(() -> new DailyCompletionException(STATE_CHANGED));
        Instant eligibleFrom = requestedDate.atStartOfDay(zone).toInstant();
        if (assignment.getAssignedAt().isAfter(eligibleFrom)) eligibleFrom = assignment.getAssignedAt();
        if (participant.getJoinedAt().isAfter(eligibleFrom)) eligibleFrom = participant.getJoinedAt();
        Instant deadline = requestedDate.plusDays(1).atStartOfDay(zone).toInstant();
        if (!eligibleFrom.isBefore(deadline)) {
            throw new DailyCompletionException(NOT_ELIGIBLE_FOR_DAY);
        }
        DailyProgress record = progress.findProgress(assignment.getId(), guildId, discordUserId).orElse(null);
        DailyCompletionResultDto existing = record != null && record.getStatus() == DailyProgressStatus.COMPLETED
                ? result(record, assignment, DailyCompletionOutcome.ALREADY_COMPLETED) : null;
        var check = new DailyCompletionCheckDto(challengeId, guildId, discordUserId, participant.getId(),
                assignment.getId(), requestedDate, problem.getId(), problem.getTitleSlug(), account.getId(),
                account.getLeetcodeUsername(), account.getVerifiedAt(), eligibleFrom, deadline, existing);
        return new State(check, participant, assignment, record);
    }

    private boolean sameTarget(DailyCompletionCheckDto expected, DailyCompletionCheckDto current) {
        return Objects.equals(expected.getParticipantId(), current.getParticipantId())
                && Objects.equals(expected.getAssignmentId(), current.getAssignmentId())
                && Objects.equals(expected.getProblemId(), current.getProblemId())
                && Objects.equals(expected.getTitleSlug(), current.getTitleSlug())
                && Objects.equals(expected.getAccountId(), current.getAccountId())
                && expected.getUsername().equalsIgnoreCase(current.getUsername())
                && Objects.equals(expected.getAccountVerifiedAt(), current.getAccountVerifiedAt())
                && Objects.equals(expected.getEligibleFrom(), current.getEligibleFrom())
                && Objects.equals(expected.getDeadline(), current.getDeadline());
    }

    private DailyCompletionResultDto result(DailyProgress record, DailyChallenge assignment,
                                           DailyCompletionOutcome outcome) {
        return new DailyCompletionResultDto(record.getChallengeId(), assignment.getId(), assignment.getAssignmentDate(),
                record.getId(), outcome, record.getSubmissionId(), record.getCompletedAt());
    }

    @RequiredArgsConstructor
    private static class State {
        private final DailyCompletionCheckDto check;
        private final ChallengeParticipant participant;
        private final DailyChallenge assignment;
        private final DailyProgress progress;
    }
}
