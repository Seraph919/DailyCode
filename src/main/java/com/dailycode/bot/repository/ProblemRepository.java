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

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findByLeetCodeQuestionId(String leetCodeQuestionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select problem from Problem problem where problem.leetCodeQuestionId = :questionId")
    Optional<Problem> findForSync(@Param("questionId") String questionId);

    Optional<Problem> findByTitleSlug(String titleSlug);

    Page<Problem> findByDifficultyAndPaidOnlyFalseOrderByIdAsc(ProblemDifficulty difficulty, Pageable pageable);
}
