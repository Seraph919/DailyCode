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
}
