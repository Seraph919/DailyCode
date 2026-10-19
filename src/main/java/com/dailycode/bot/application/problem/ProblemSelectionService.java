package com.dailycode.bot.application.problem;

import com.dailycode.bot.application.problem.dto.SelectedProblemDto;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.repository.ProblemRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.Set;

@Service
@Validated
@RequiredArgsConstructor
public class ProblemSelectionService {

    private final ProblemRepository problems;

    /** Caller must hold the challenge write lock until the assignment commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Problem> selectForAssignment(@NotNull @Positive Long challengeId,
                                                 @NotNull ProblemDifficulty difficulty) {
        return problems.findUnusedForAssignment(challengeId, difficulty, PageRequest.of(0, 1))
                .stream().findFirst();
    }

    /**
     * Selects the lowest-ID free problem at the requested difficulty.
     * The caller supplies internal problem IDs already used in one challenge.
     * Selection does not reserve a problem or record an assignment.
     */
    @Transactional(readOnly = true)
    public Optional<SelectedProblemDto> select(
            @NotNull ProblemDifficulty difficulty,
            @NotNull Set<@NotNull @Positive Long> usedProblemIds) {
        Set<Long> excludedIds = Set.copyOf(usedProblemIds);
        // Avoid an empty NOT IN clause; both queries filter and limit in the database.
        Optional<Problem> selected = excludedIds.isEmpty()
                ? problems.findFirstByDifficultyAndPaidOnlyFalseOrderByIdAsc(difficulty)
                : problems.findFirstByDifficultyAndPaidOnlyFalseAndIdNotInOrderByIdAsc(difficulty, excludedIds);
        return selected.map(this::toDto);
    }

    private SelectedProblemDto toDto(Problem problem) {
        return new SelectedProblemDto(problem.getId(), problem.getFrontendId(), problem.getTitle(),
                problem.getTitleSlug(), problem.getDifficulty());
    }
}
