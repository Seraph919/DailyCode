package com.dailycode.bot.application.problem;

import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.dailycode.bot.application.problem.dto.ProblemSyncResultDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NEVER)
public class ProblemSyncService {

    private final LeetCodeClient client;
    private final ProblemSyncTransaction syncTransaction;

    public ProblemSyncResultDto sync(String titleSlug) {
        return sync(titleSlug, false);
    }

    public ProblemSyncResultDto sync(String titleSlug, boolean includePaid) {
        // Fetch once, outside any database transaction. Invalid slugs fail in the client.
        LeetCodeProblemDto metadata = client.fetchProblem(titleSlug);
        try {
            return syncTransaction.save(metadata, includePaid);
        } catch (DataIntegrityViolationException failure) {
            if (!isDuplicateProblem(failure)) {
                throw failure;
            }
            // A concurrent insert may have won. Retry once after rollback, without refetching.
            // A slug owned by a different question still fails; it must never be reassigned.
            return syncTransaction.save(metadata, includePaid);
        }
    }

    private boolean isDuplicateProblem(DataIntegrityViolationException failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && ("uk_problems_leetcode_question_id".equals(violation.getConstraintName())
                    || "uk_problems_title_slug".equals(violation.getConstraintName()))) {
                return true;
            }
        }
        return false;
    }
}
