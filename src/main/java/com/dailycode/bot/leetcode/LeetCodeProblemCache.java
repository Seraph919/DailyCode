package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeetCodeProblemCache {

    private final LeetCodeClient client;
    private final Cache<String, LeetCodeProblemDto> problemMetadataCache;

    public LeetCodeProblemDto get(String titleSlug) {
        // Caffeine coalesces simultaneous misses for the same slug into one fetch.
        return problemMetadataCache.get(normalize(titleSlug), client::fetchProblem);
    }

    public LeetCodeProblemDto refresh(String titleSlug) {
        // Serialize refreshes with loads for this key, preventing an older in-flight
        // load from overwriting the refreshed value. Failures propagate to the caller.
        return problemMetadataCache.asMap().compute(normalize(titleSlug), this::fetchFresh);
    }

    private LeetCodeProblemDto fetchFresh(String titleSlug, LeetCodeProblemDto previous) {
        return client.fetchProblem(titleSlug);
    }

    private String normalize(String titleSlug) {
        if (titleSlug == null || titleSlug.strip().length() > 300
                || !titleSlug.strip().matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Provide a valid LeetCode problem slug of at most 300 characters.");
        }
        return titleSlug.strip();
    }
}
