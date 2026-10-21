package com.dailycode.bot.repository;

import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select problem from Problem problem where problem.id = :id")
    Optional<Problem> findForCompletion(@Param("id") Long id);

    Optional<Problem> findByLeetCodeQuestionId(String leetCodeQuestionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select problem from Problem problem where problem.leetCodeQuestionId = :questionId")
    Optional<Problem> findForSync(@Param("questionId") String questionId);

    Optional<Problem> findByTitleSlug(String titleSlug);

    boolean existsByTitleSlug(String titleSlug);

    Optional<Problem> findFirstByDifficultyAndPaidOnlyFalseOrderByIdAsc(ProblemDifficulty difficulty);

    Optional<Problem> findFirstByDifficultyAndPaidOnlyFalseAndIdNotInOrderByIdAsc(
            ProblemDifficulty difficulty, Collection<Long> excludedIds);

    // Hold eligible metadata stable until the assignment commits; sync uses a write lock.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
            select problem from Problem problem
            where problem.difficulty = :difficulty and problem.paidOnly = false
              and not exists (
                select daily.id from DailyChallenge daily
                where daily.challenge.id = :challengeId and daily.problem.id = problem.id
              )
            order by problem.id
            """)
    List<Problem> findUnusedForAssignment(@Param("challengeId") Long challengeId,
                                          @Param("difficulty") ProblemDifficulty difficulty,
                                          Pageable pageable);

    Page<Problem> findByDifficultyAndPaidOnlyFalseOrderByIdAsc(ProblemDifficulty difficulty, Pageable pageable);
}
