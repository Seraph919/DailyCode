package com.dailycode.bot.application.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeVerificationProfileDto;
import com.dailycode.bot.domain.leetcode.LeetCodeAccount;
import com.dailycode.bot.domain.user.DiscordUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeetCodeOwnershipProofCheckerTests {

    private final LeetCodeOwnershipProofChecker checker = new LeetCodeOwnershipProofChecker();
    private final DiscordUser user = new DiscordUser("123456789012345678", "alice");
    private LeetCodeAccount account;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        account = new LeetCodeAccount(user, "alice_lc");
        account.startVerification("dailycode-token123", now.plus(10, ChronoUnit.MINUTES), now);
    }

    @Test
    void rejectsNullAccountOrNow() {
        assertThatThrownBy(() -> checker.hasValidProof(null, new LeetCodeVerificationProfileDto("alice_lc", "token"), now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> checker.hasValidProof(account, new LeetCodeVerificationProfileDto("alice_lc", "token"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsTrueWhenTokenMatchesOnItsOwnLine() {
        var profile = new LeetCodeVerificationProfileDto("alice_lc", "dailycode-token123");
        assertThat(checker.hasValidProof(account, profile, now)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "First line\ndailycode-token123\nThird line",
            "Header\r\ndailycode-token123\r\nFooter",
            "dailycode-token123\nNext line"
    })
    void returnsTrueWhenTokenAppearsAmongMultilineContent(String aboutMe) {
        var profile = new LeetCodeVerificationProfileDto("alice_lc", aboutMe);
        assertThat(checker.hasValidProof(account, profile, now)).isTrue();
    }

    @Test
    void caseInsensitiveUsernameMatch() {
        var profile = new LeetCodeVerificationProfileDto("ALICE_LC", "dailycode-token123");
        assertThat(checker.hasValidProof(account, profile, now)).isTrue();
    }

    @Test
    void returnsFalseWhenTokenIsEmbeddedInALine() {
        var profile = new LeetCodeVerificationProfileDto("alice_lc", "my token is dailycode-token123 please verify");
        assertThat(checker.hasValidProof(account, profile, now)).isFalse();
    }

    @Test
    void returnsFalseWhenUsernameMismatched() {
        var profile = new LeetCodeVerificationProfileDto("bob_lc", "dailycode-token123");
        assertThat(checker.hasValidProof(account, profile, now)).isFalse();
    }

    @Test
    void returnsFalseWhenChallengeIsExpired() {
        Instant pastExpiry = now.plus(15, ChronoUnit.MINUTES);
        var profile = new LeetCodeVerificationProfileDto("alice_lc", "dailycode-token123");
        assertThat(checker.hasValidProof(account, profile, pastExpiry)).isFalse();
    }

    @Test
    void returnsFalseWhenProfileOrAboutMeIsNull() {
        assertThat(checker.hasValidProof(account, null, now)).isFalse();
        assertThat(checker.hasValidProof(account, new LeetCodeVerificationProfileDto(null, "token"), now)).isFalse();
        assertThat(checker.hasValidProof(account, new LeetCodeVerificationProfileDto("alice_lc", null), now)).isFalse();
    }
}
