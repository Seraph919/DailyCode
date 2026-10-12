package com.dailycode.bot;

import com.dailycode.bot.support.PostgresTestConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
class DailyCodeApplicationTests {

    @Test
    void connectsToPostgresThroughJpa(@Autowired EntityManager entityManager) {
        Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
        assertThat(result).isEqualTo(1);

        Object databaseName = entityManager.createNativeQuery("SELECT current_database()").getSingleResult();
        assertThat(databaseName).isEqualTo("dailycode_test");
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
