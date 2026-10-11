package com.dailycode.bot.application.user;

import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DiscordUserRegistrationTransaction {

    private final DiscordUserRepository repository;

    @Transactional
    public DiscordUser register(String discordUserId, String discordUsername) {
        return repository.findByDiscordUserId(discordUserId)
                .map(user -> {
                    user.changeUsername(discordUsername);
                    return user;
                })
                .orElseGet(() -> repository.save(new DiscordUser(discordUserId, discordUsername)));
    }
}
