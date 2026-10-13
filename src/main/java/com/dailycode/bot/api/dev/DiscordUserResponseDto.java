package com.dailycode.bot.api.dev;

import com.dailycode.bot.domain.user.DiscordUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscordUserResponseDto {

    private Long id;
    private String discordUserId;
    private String discordUsername;
    private Instant createdAt;
    private Instant updatedAt;

    public static DiscordUserResponseDto from(DiscordUser user) {
        return new DiscordUserResponseDto(user.getId(), user.getDiscordUserId(),
                user.getDiscordUsername(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
