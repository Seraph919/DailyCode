package com.dailycode.bot.leetcode;

import com.dailycode.bot.application.leetcode.dto.LeetCodeProblemDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ProblemCacheConfiguration {

    @Bean
    public Cache<String, LeetCodeProblemDto> problemMetadataCache(
            @Value("${leetcode.problem-cache.ttl:PT1H}") Duration ttl,
            @Value("${leetcode.problem-cache.maximum-size:1000}") long maximumSize) {
        if (ttl.isNegative() || ttl.isZero() || maximumSize <= 0) {
            throw new IllegalArgumentException("Problem cache TTL and maximum size must be positive.");
        }
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .build();
    }
}
