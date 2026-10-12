package com.dailycode.bot.application.user;

import com.dailycode.bot.support.PostgresTestConfiguration;
import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(PostgresTestConfiguration.class)
@SpringBootTest(properties = "spring.profiles.active=test")
class DiscordUserRegistrationConcurrencyTests {

    @MockitoSpyBean
    private DiscordUserRepository repository;

    @Test
    void concurrentFirstRegistrationsReturnTheSameRow(@Autowired DiscordUserService service) throws Exception {
        String discordId = "race-" + Long.toUnsignedString(System.nanoTime(), 36);
        CountDownLatch bothLookedUp = new CountDownLatch(2);
        AtomicInteger lookups = new AtomicInteger();
        var repositoryDelegate = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object result = repositoryDelegate.answer(invocation);
            // Force both transactions to see absence before either tries inserting.
            if (lookups.incrementAndGet() <= 2) {
                bothLookedUp.countDown();
                if (!bothLookedUp.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The second registration never reached its lookup");
                }
            }
            return result;
        }).when(repository).findByDiscordUserId(discordId);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> service.register(discordId, "race-test"));
            var second = executor.submit(() -> service.register(discordId, "race-test"));
            DiscordUser firstResult = first.get(20, TimeUnit.SECONDS);
            DiscordUser secondResult = second.get(20, TimeUnit.SECONDS);

            assertThat(firstResult.getId()).isEqualTo(secondResult.getId());
            assertThat(repository.findByDiscordUserId(discordId)).hasValueSatisfying(user ->
                    assertThat(user.getId()).isEqualTo(firstResult.getId()));
            verify(repository, times(2)).save(any(DiscordUser.class));
        } finally {
            // These worker transactions really commit, so remove only this test's generated row.
            repository.findByDiscordUserId(discordId).ifPresent(repository::delete);
        }
    }
}
