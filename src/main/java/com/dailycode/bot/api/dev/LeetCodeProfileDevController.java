package com.dailycode.bot.api.dev;

import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Profile("dev")
@RequestMapping("/api/dev/leetcode")
@RequiredArgsConstructor
public class LeetCodeProfileDevController {

    private final LeetCodeClient client;

    @Operation(summary = "Look up a public LeetCode profile",
            description = "Local development only. Does not link accounts or verify ownership.")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "400", description = "Missing, blank, or overlong username", content = @Content)
    @ApiResponse(responseCode = "502", description = "LeetCode lookup failed", content = @Content)
    @GetMapping("/profile")
    public LeetCodeUserProfileDto profile(
            @Parameter(example = "Seraph919")
            @RequestParam("username") @NotBlank @Size(max = 100) String username) {
        return client.fetchProfile(username);
    }

    @ExceptionHandler(LeetCodeClientException.class)
    public ProblemDetail handleLookupFailure(LeetCodeClientException failure) {
        log.warn("Development LeetCode profile lookup failed.", failure);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Could not retrieve the LeetCode profile. Check the username and try again later.");
    }
}
