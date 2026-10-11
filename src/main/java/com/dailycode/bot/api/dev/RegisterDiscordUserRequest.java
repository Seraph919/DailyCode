package com.dailycode.bot.api.dev;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDiscordUserRequest {

    @NotBlank
    @Size(max = 20)
    @Schema(example = "987654321098765432")
    private String discordUserId;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "alice")
    private String discordUsername;
}