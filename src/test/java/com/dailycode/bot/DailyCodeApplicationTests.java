package com.dailycode.bot;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
class DailyCodeApplicationTests {

    @Test
    void connectsToPostgresThroughJpa(@Autowired EntityManager entityManager) {
        Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
        assertThat(result).isEqualTo(1);
    }

    @Test
    void firstMigrationCreatesDiscordUsers(@Autowired EntityManager entityManager) {
        Number appliedCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE version = '1' AND success = true
                """).getSingleResult();
        assertThat(appliedCount.longValue()).isEqualTo(1L);

        // Check that every planned column exists without reading or inserting user data.
        assertThat(entityManager.createNativeQuery("""
                SELECT id, discord_user_id, discord_username, created_at, updated_at
                FROM discord_users WHERE false
                """).getResultList()).isEmpty();
    }
}
