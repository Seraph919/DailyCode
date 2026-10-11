package com.dailycode.bot.application.user;

import com.dailycode.bot.domain.user.DiscordUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class DiscordUserService {

    private final DiscordUserRegistrationTransaction registrationTransaction;

    public DiscordUser register(
            @NotBlank @Size(max = 20) String discordUserId,
            @NotBlank @Size(max = 100) String discordUsername) {
        try {
            return registrationTransaction.register(discordUserId, discordUsername);
        } catch (DataIntegrityViolationException exception) {
            // An enclosing transaction belongs to the caller and cannot be retried here.
            if (TransactionSynchronizationManager.isActualTransactionActive()
                    || !isDuplicateDiscordId(exception)) {
                throw exception;
            }
            // The transactional bean has already rolled back. Retry once in a fresh transaction.
            return registrationTransaction.register(discordUserId, discordUsername);
        }
    }

    private boolean isDuplicateDiscordId(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "uk_discord_users_discord_user_id".equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
