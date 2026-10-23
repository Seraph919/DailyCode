package com.dailycode.bot.application.leetcode;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationTokenGeneratorTests {

    private final VerificationTokenGenerator generator = new VerificationTokenGenerator();

    @Test
    void generatesExpectedFormat() {
        String token = generator.generateToken();

        assertThat(token).startsWith("dailycode-");
        // "dailycode-" (10 chars) + 16 bytes formatted as 32 hex chars = 42 chars
        assertThat(token).hasSize(42);
        assertThat(token.substring("dailycode-".length())).matches("[0-9a-f]{32}");
    }

    @Test
    void generatesUniqueTokens() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            assertThat(seen.add(generator.generateToken())).isTrue();
        }
    }
}
