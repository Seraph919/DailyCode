package com.dailycode.bot.config;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("db-check")
public class DatabaseConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionCheck.class);

    private final EntityManager entityManager;

    public DatabaseConnectionCheck(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        Object result = entityManager.createNativeQuery("SELECT 1").getSingleResult();
        log.info("JPA database connection verified (SELECT 1 = {}).", result);
    }
}
