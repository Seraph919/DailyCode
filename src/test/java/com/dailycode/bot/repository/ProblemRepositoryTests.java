package com.dailycode.bot.repository;

import com.dailycode.bot.domain.challenge.Challenge;
import com.dailycode.bot.domain.challenge.DailyChallenge;
import com.dailycode.bot.domain.problem.Problem;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ProblemRepositoryTests {

    @Autowired
    private ProblemRepository problems;

    @Autowired
    private EntityManager em;

    @Test
    void findsByQuestionIdAndSlug() {
        Problem problem = new Problem("101", "101", "Binary Search", "binary-search", ProblemDifficulty.EASY, false);
        em.persist(problem);
        em.flush();

        Optional<Problem> byQId = problems.findByLeetCodeQuestionId("101");
        assertThat(byQId).isPresent();
        assertThat(byQId.get().getTitle()).isEqualTo("Binary Search");

        Optional<Problem> bySlug = problems.findByTitleSlug("binary-search");
        assertThat(bySlug).isPresent();
        assertThat(bySlug.get().getLeetCodeQuestionId()).isEqualTo("101");

        assertThat(problems.existsByTitleSlug("binary-search")).isTrue();
        assertThat(problems.existsByTitleSlug("linear-search")).isFalse();
    }

    @Test
    void findUnusedForAssignmentExcludesAssignedProblems() {
        DiscordUser creator = new DiscordUser("123456789012345613", "creator13");
        em.persist(creator);
        Challenge challenge = new Challenge("100", "Problem Challenge", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "UTC");
        em.persist(challenge);

        Problem p1 = new Problem("201", "201", "Prob 1", "prob-1", ProblemDifficulty.EASY, false);
        Problem p2 = new Problem("202", "202", "Prob 2", "prob-2", ProblemDifficulty.EASY, false);
        Problem pPaid = new Problem("203", "203", "Prob 3 Paid", "prob-3-paid", ProblemDifficulty.EASY, true);
        Problem pMedium = new Problem("204", "204", "Prob 4 Med", "prob-4-med", ProblemDifficulty.MEDIUM, false);
        em.persist(p1);
        em.persist(p2);
        em.persist(pPaid);
        em.persist(pMedium);
        em.flush();

        // Assign p1 to challenge
        DailyChallenge daily = new DailyChallenge(challenge, p1, LocalDate.of(2026, 1, 1));
        em.persist(daily);
        em.flush();

        List<Problem> unused = problems.findUnusedForAssignment(
                challenge.getId(), ProblemDifficulty.EASY, PageRequest.of(0, 10));

        // Should contain p2, but not p1 (already assigned), not pPaid (paid only), and not pMedium (different difficulty)
        assertThat(unused).extracting(Problem::getTitleSlug).containsExactly("prob-2");
    }
}
