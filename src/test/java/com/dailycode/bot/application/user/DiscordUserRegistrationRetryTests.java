package com.dailycode.bot.application.user;

import com.dailycode.bot.domain.user.DiscordUser;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DiscordUserRegistrationRetryTests {

    private final DiscordUserRegistrationTransaction transaction = mock(DiscordUserRegistrationTransaction.class);
    private final DiscordUserService service = new DiscordUserService(transaction);

    @Test
    void retriesTheDiscordIdConflictOnce() {
        DiscordUser winner = new DiscordUser("123", "alice");
        when(transaction.register("123", "alice"))
                .thenThrow(conflict("uk_discord_users_discord_user_id"))
                .thenReturn(winner);

        assertThat(service.register("123", "alice")).isSameAs(winner);
        verify(transaction, times(2)).register("123", "alice");
    }

    @Test
    void doesNotRetryIndefinitely() {
        DataIntegrityViolationException failure = conflict("uk_discord_users_discord_user_id");
        when(transaction.register("123", "alice")).thenThrow(failure);

        assertThatThrownBy(() -> service.register("123", "alice")).isSameAs(failure);
        verify(transaction, times(2)).register("123", "alice");
    }

    @Test
    void doesNotRetryAnUnrelatedConstraint() {
        DataIntegrityViolationException failure = conflict("another_constraint");
        when(transaction.register("123", "alice")).thenThrow(failure);

        assertThatThrownBy(() -> service.register("123", "alice")).isSameAs(failure);
        verify(transaction).register("123", "alice");
    }

    @Test
    void leavesAnEnclosingTransactionToItsCaller() {
        DataIntegrityViolationException failure = conflict("uk_discord_users_discord_user_id");
        when(transaction.register("123", "alice")).thenThrow(failure);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> service.register("123", "alice")).isSameAs(failure);
            verify(transaction).register("123", "alice");
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    private DataIntegrityViolationException conflict(String constraint) {
        return new DataIntegrityViolationException("Unique constraint failed",
                new ConstraintViolationException("Duplicate value", new SQLException("Duplicate", "23505"), constraint));
    }
}
