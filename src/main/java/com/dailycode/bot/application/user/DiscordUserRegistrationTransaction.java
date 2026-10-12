package com.dailycode.bot.application.user;

import com.dailycode.bot.domain.user.DiscordUser;
import com.dailycode.bot.repository.DiscordUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DiscordUserRegistrationTransaction {

    private final DiscordUserRepository repository;

    @Transactional
    public DiscordUser register(String discordUserId, String discordUsername) {
        Optional<DiscordUser> existingUser = repository.findByDiscordUserId(discordUserId);

        if (existingUser.isPresent()) {
            DiscordUser user = existingUser.get();
            user.changeUsername(discordUsername);
            return user;
        }

        DiscordUser newUser = new DiscordUser(discordUserId, discordUsername);
        return repository.save(newUser);
    }
}
