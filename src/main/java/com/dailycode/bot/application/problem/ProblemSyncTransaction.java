package com.dailycode.bot.application.problem;

import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.dailycode.bot.application.problem.dto.ProblemSyncResultDto;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProblemSyncTransaction {

    private final ProblemRepository problems;

    @Transactional
    public ProblemSyncResultDto save(LeetCodeProblemDto metadata, boolean includePaid) {
        var existing = problems.findForSync(metadata.getLeetCodeQuestionId());
        if (existing.isPresent()) {
            Problem problem = existing.get();
            // Refresh premium status too, so a formerly free problem stops being selected.
            problem.updateMetadata(metadata.getFrontendId(), metadata.getTitle(), metadata.getTitleSlug(),
                    metadata.getDifficulty(), metadata.isPaidOnly());
            problems.flush();
            return new ProblemSyncResultDto(ProblemSyncStatus.REFRESHED, problem.getId(), problem.getTitleSlug());
        }
        if (metadata.isPaidOnly() && !includePaid) {
            return new ProblemSyncResultDto(ProblemSyncStatus.SKIPPED_PAID, null, metadata.getTitleSlug());
        }

        Problem problem = problems.saveAndFlush(new Problem(metadata.getLeetCodeQuestionId(), metadata.getFrontendId(),
                metadata.getTitle(), metadata.getTitleSlug(), metadata.getDifficulty(), metadata.isPaidOnly()));
        return new ProblemSyncResultDto(ProblemSyncStatus.CREATED, problem.getId(), problem.getTitleSlug());
    }
}
