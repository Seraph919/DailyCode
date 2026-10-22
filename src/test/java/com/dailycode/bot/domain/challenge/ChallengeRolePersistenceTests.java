package com.dailycode.bot.domain.challenge;

import com.dailycode.bot.application.challenge.ChallengeAutomationService;
import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class ChallengeRolePersistenceTests {
    @Test
    void automationPersistsReplacesAndClearsRole(@Autowired EntityManager entities,
                                                @Autowired ChallengeAutomationService automation) {
        var creator = new DiscordUser("123456789012345690", "role-test");
        entities.persist(creator);
        var challenge = new Challenge("100", "Practice", creator,
                LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31), "UTC");
        entities.persist(challenge);
        entities.flush();
        Long id = challenge.getId();
        entities.clear();
        assertThat(entities.find(Challenge.class, id).getAnnouncementRoleId()).isNull();

        for (String roleId : new String[]{"300", "301", null}) {
            automation.enable(id, "100", "200", ProblemDifficulty.EASY, LocalTime.MIDNIGHT, roleId);
            entities.flush();
            entities.clear();
            assertThat(entities.find(Challenge.class, id).getAnnouncementRoleId()).isEqualTo(roleId);
        }
    }
}
