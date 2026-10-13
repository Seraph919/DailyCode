package com.dailycode.bot.api.dev;

import com.dailycode.bot.application.user.DiscordUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/dev/discord-users")
@RequiredArgsConstructor
public class DiscordUserDevController {

    private final DiscordUserService service;

    @Operation(summary = "Register a user or refresh their username",
            description = "Local development only. Does not verify ownership of the supplied Discord ID.")
    @ApiResponse(responseCode = "200", description = "User registered or refreshed")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content)
    @PostMapping
    public DiscordUserResponseDto register(@Valid @RequestBody RegisterDiscordUserRequestDto request) {
        return DiscordUserResponseDto.from(service.register(request.getDiscordUserId(), request.getDiscordUsername()));
    }
}
