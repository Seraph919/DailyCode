package com.dailycode.bot.application.challenge;

import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.repository.ChallengeRepository;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ChallengeGuildIsolationTests {
    private static final String USER = "123456789012345699";
    @Autowired EntityManager entities;
    @Autowired Clock clock;
    @Autowired ChallengeService challenges;
    @Autowired ChallengeJoinTransaction joins;
    @Autowired ChallengeAutomationService automation;
    @Autowired ChallengeTodayService today;
    @Autowired ChallengeProgressService progress;
    @Autowired DailyAssignmentService assignments;
    @Autowired DailyCompletionTransaction completions;
    @Autowired ChallengeRepository repository;
    private Long first;
    private Long second;

    @BeforeEach
    void twoServersShareAUserButHaveSeparateChallenges() {
        var user = new DiscordUser(USER, "multi-server-user");
        entities.persist(user);
        var account = new LeetCodeAccount(user, "multi-server-user");
        account.startVerification("proof", clock.instant().plusSeconds(600), clock.instant());
        account.completeVerification("proof", clock.instant());
        entities.persist(account);
        entities.flush();
        var end = LocalDate.now(clock.withZone(ZoneOffset.UTC)).plusDays(30);
        first = challenges.create("100", USER, "Practice", null, end, "UTC").getId();
        second = challenges.create("200", USER, "Practice", null, end, "UTC").getId();
        joins.join(first, "100", USER);
        joins.join(second, "200", USER);
        automation.enable(first, "100", "101", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, "102");
        automation.enable(second, "200", "201", ProblemDifficulty.HARD, LocalTime.NOON, "202");
    }

    @Test
    void sameUserCanParticipateInBothServersWithIndependentSettings() {
        assertThat(first).isNotEqualTo(second);
        assertThat(progress.status(first, "100", USER, 1).challengeId()).isEqualTo(first);
        assertThat(progress.status(second, "200", USER, 1).challengeId()).isEqualTo(second);
        assertThat(automation.targets(0)).anySatisfy(target -> {
            assertThat(target.getChallengeId()).isEqualTo(first);
            assertThat(target.getGuildId()).isEqualTo("100");
            assertThat(target.getChannelId()).isEqualTo("101");
        }).anySatisfy(target -> {
            assertThat(target.getChallengeId()).isEqualTo(second);
            assertThat(target.getGuildId()).isEqualTo("200");
            assertThat(target.getChannelId()).isEqualTo("201");
        });
        automation.disable(first, "100");
        entities.flush();
        entities.clear();
        assertThat(repository.findById(first).orElseThrow().getAnnouncementChannelId()).isNull();
        var other = repository.findById(second).orElseThrow();
        assertThat(other.getAnnouncementChannelId()).isEqualTo("201");
        assertThat(other.getAnnouncementRoleId()).isEqualTo("202");
        assertThat(other.getAutomationDifficulty()).isEqualTo(ProblemDifficulty.HARD);
    }

    @Test
    void anotherServerCannotReadOrJoinAChallengeEvenForTheSameUser() {
        assertThatThrownBy(() -> joins.join(first, "200", USER))
                .isInstanceOfSatisfying(ChallengeJoinException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(ChallengeJoinException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> today.today(first, "200"))
                .isInstanceOfSatisfying(ChallengeTodayException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(ChallengeTodayException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> progress.status(first, "200", USER, 1))
                .isInstanceOfSatisfying(DailyCompletionException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> completions.prepare(first, "200", USER, null))
                .isInstanceOfSatisfying(DailyCompletionException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyCompletionException.Reason.CHALLENGE_NOT_FOUND));
    }

    @Test
    void anotherServerCannotChangeAutomationOrAssignProblems() {
        assertThatThrownBy(() -> automation.enable(first, "200", "201", ProblemDifficulty.HARD, LocalTime.NOON, "202"))
                .isInstanceOfSatisfying(DailyAssignmentException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyAssignmentException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> automation.disable(first, "200"))
                .isInstanceOfSatisfying(DailyAssignmentException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyAssignmentException.Reason.CHALLENGE_NOT_FOUND));
        assertThatThrownBy(() -> assignments.assignToday(first, "200", ProblemDifficulty.EASY))
                .isInstanceOfSatisfying(DailyAssignmentException.class,
                        failure -> assertThat(failure.getReason()).isEqualTo(DailyAssignmentException.Reason.CHALLENGE_NOT_FOUND));
        assertThat(repository.findById(first).orElseThrow().getAnnouncementChannelId()).isEqualTo("101");
    }
}
