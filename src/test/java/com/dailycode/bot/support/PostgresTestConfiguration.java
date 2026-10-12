package com.dailycode.bot.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:17-bookworm")
                .withDatabaseName("dailycode_test")
                .withUsername("test")
                .withPassword("test"); // test credentials, unrelated to .env.
    }
}
