package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeAcceptedSubmissionDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmissionEligibilityCheckerTests {

    private final SubmissionEligibilityChecker checker = new SubmissionEligibilityChecker();

    private final LocalDate assignmentDate = LocalDate.of(2026, 10, 23);
    private final Instant baseTime = Instant.parse("2026-10-23T12:00:00Z");
    private final Instant eligibleFrom = Instant.parse("2026-10-23T00:00:00Z");
    private final Instant deadline = Instant.parse("2026-10-24T00:00:00Z");

    private final DailyCompletionCheckDto check = new DailyCompletionCheckDto(
            1L, "100", "123456789012345678", 10L, 100L, assignmentDate,
            1000L, "two-sum", 50L, "alice_lc", eligibleFrom.minusSeconds(86400),
            eligibleFrom, deadline, null);

    @Test
    void findsEarliestEligibleSubmission() {
        var sub1 = new LeetCodeAcceptedSubmissionDto("101", "Two Sum", "two-sum", baseTime.plusSeconds(3600));
        var sub2 = new LeetCodeAcceptedSubmissionDto("100", "Two Sum", "two-sum", baseTime); // earlier
        var feed = new LeetCodeRecentSubmissionsDto("alice_lc", List.of(sub1, sub2));

        Optional<LeetCodeAcceptedSubmissionDto> eligible = checker.findEligible(check, feed, baseTime.plusSeconds(7200));

        assertThat(eligible).isPresent();
        assertThat(eligible.get().getSubmissionId()).isEqualTo("100");
    }

    @Test
    void ignoresSubmissionsOutsideWindowOrForOtherProblem() {
        var beforeWindow = new LeetCodeAcceptedSubmissionDto("90", "Two Sum", "two-sum", eligibleFrom.minusSeconds(1));
        var afterDeadline = new LeetCodeAcceptedSubmissionDto("91", "Two Sum", "two-sum", deadline.plusSeconds(1));
        var futureSubmission = new LeetCodeAcceptedSubmissionDto("92", "Two Sum", "two-sum", baseTime.plusSeconds(600));
        var wrongProblem = new LeetCodeAcceptedSubmissionDto("93", "Three Sum", "three-sum", baseTime);

        var feed = new LeetCodeRecentSubmissionsDto("alice_lc", List.of(beforeWindow, afterDeadline, futureSubmission, wrongProblem));

        Optional<LeetCodeAcceptedSubmissionDto> eligible = checker.findEligible(check, feed, baseTime);

        assertThat(eligible).isEmpty();
    }

    @Test
    void throwsWhenFeedUsernameMismatched() {
        var feed = new LeetCodeRecentSubmissionsDto("bob_lc", List.of());

        assertThatThrownBy(() -> checker.findEligible(check, feed, baseTime))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("The submission feed does not match the verified account.");
    }

    @Test
    void throwsWhenSubmissionEvidenceInvalid() {
        var invalidSub = new LeetCodeAcceptedSubmissionDto(null, "Two Sum", "two-sum", baseTime);
        var feed = new LeetCodeRecentSubmissionsDto("alice_lc", List.of(invalidSub));

        assertThatThrownBy(() -> checker.findEligible(check, feed, baseTime))
                .isInstanceOf(LeetCodeClientException.class)
                .hasMessage("The submission feed contains invalid evidence.");
    }
}
