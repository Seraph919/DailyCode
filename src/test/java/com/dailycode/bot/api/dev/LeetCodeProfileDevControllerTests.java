package com.dailycode.bot.api.dev;

import com.dailycode.bot.application.leetcode.dto.LeetCodeUserProfileDto;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.leetcode.LeetCodeClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeetCodeProfileDevControllerTests {

    private final LeetCodeClient client = mock(LeetCodeClient.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new LeetCodeProfileDevController(client)).build();
    }

    @Test
    void returnsProfileSuccessfully() throws Exception {
        LeetCodeUserProfileDto profile = new LeetCodeUserProfileDto();
        profile.setUsername("Seraph919");
        profile.setRanking(12345);

        when(client.fetchProfile("Seraph919")).thenReturn(profile);

        mvc.perform(get("/api/dev/leetcode/profile").param("username", "Seraph919"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Seraph919"))
                .andExpect(jsonPath("$.ranking").value(12345));

        verify(client).fetchProfile("Seraph919");
    }

    @Test
    void returnsBadGatewayOnClientException() throws Exception {
        when(client.fetchProfile("ghost")).thenThrow(new LeetCodeClientException("Not found"));

        mvc.perform(get("/api/dev/leetcode/profile").param("username", "ghost"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(
                        "Could not retrieve the LeetCode profile. Check the username and try again later."));
    }
}

