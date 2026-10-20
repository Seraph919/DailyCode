package com.dailycode.bot.application.challenge;

import com.dailycode.bot.application.challenge.dto.DailyCompletionCheckDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeAcceptedSubmissionDto;
import com.dailycode.bot.application.leetcode.dto.LeetCodeRecentSubmissionsDto;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SubmissionEligibilityChecker {

    public Optional<LeetCodeAcceptedSubmissionDto> findEligible(DailyCompletionCheckDto check,
                                                               LeetCodeRecentSubmissionsDto feed,
                                                               Instant now) {
        if (feed == null || !check.getUsername().equalsIgnoreCase(feed.getUsername())) {
            throw new LeetCodeClientException("The submission feed does not match the verified account.");
        }
        LeetCodeAcceptedSubmissionDto earliest = null;
        for (LeetCodeAcceptedSubmissionDto submission : feed.getSubmissions()) {
            Instant submittedAt = submission.getSubmittedAt();
            if (submission.getSubmissionId() == null || !submission.getSubmissionId().matches("[1-9][0-9]{0,49}")
                    || submission.getTitleSlug() == null || submittedAt == null) {
                throw new LeetCodeClientException("The submission feed contains invalid evidence.");
            }
            if (!check.getTitleSlug().equals(submission.getTitleSlug())
                    || submittedAt.isBefore(check.getEligibleFrom())
                    || !submittedAt.isBefore(check.getDeadline())
                    || submittedAt.isAfter(now)) {
                continue;
            }
            if (earliest == null || submittedAt.isBefore(earliest.getSubmittedAt())
                    || (submittedAt.equals(earliest.getSubmittedAt())
                    && submission.getSubmissionId().compareTo(earliest.getSubmissionId()) < 0)) {
                earliest = submission;
            }
        }
        return Optional.ofNullable(earliest);
    }
}
