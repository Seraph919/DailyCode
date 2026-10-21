package com.dailycode.bot.application.problem;

import com.dailycode.bot.domain.problem.ProblemDifficulty;
import com.dailycode.bot.leetcode.LeetCodeClient;
import com.dailycode.bot.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Objects;

/** Fetch only when assignments exhaust the local catalog, outside assignment transactions. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProblemCatalogService {
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES_PER_ATTEMPT = 3;
    private static final int MAX_IMPORTS_PER_ATTEMPT = 3;
    private final LeetCodeClient client;
    private final ProblemRepository problems;
    private final ProblemSyncService sync;
    private final Clock clock;
    private final EnumMap<ProblemDifficulty, Scan> scans = new EnumMap<>(ProblemDifficulty.class);

    // Share a cursor and cooldown per difficulty across challenges in this bot instance.
    // Catalog rows persist in the database; restarting safely rescans from the beginning.
    @Transactional(propagation = Propagation.NEVER)
    public synchronized void replenish(ProblemDifficulty difficulty) {
        Objects.requireNonNull(difficulty, "Difficulty is required.");
        Scan scan = scans.computeIfAbsent(difficulty, ignored -> new Scan());
        if (clock.instant().isBefore(scan.nextAttempt)) return;
        int imports = 0;
        try {
            for (int pageNumber = 0; pageNumber < MAX_PAGES_PER_ATTEMPT; pageNumber++) {
                var page = client.fetchCatalog(difficulty, scan.offset, PAGE_SIZE);
                for (var entry : page.entries()) {
                    if (!entry.paidOnly() && !problems.existsByTitleSlug(entry.titleSlug())) {
                        // The existing sync validates full metadata and excludes premium problems.
                        // Advance only after success, so a temporary upstream failure retries this row.
                        var result = sync.sync(entry.titleSlug());
                        scan.offset++;
                        imports++;
                        if (result.getStatus() != ProblemSyncStatus.SKIPPED_PAID) {
                            log.info("Automatically imported LeetCode problem {} for {} catalog.", entry.titleSlug(), difficulty);
                            return;
                        }
                        if (imports >= MAX_IMPORTS_PER_ATTEMPT) {
                            scan.nextAttempt = clock.instant().plusSeconds(60);
                            return;
                        }
                    } else {
                        scan.offset++;
                    }
                }
                if (scan.offset >= page.total()) {
                    scan.offset = 0;
                    scan.nextAttempt = clock.instant().plus(Duration.ofHours(6));
                    log.warn("No new free {} problems found in LeetCode's catalog; will scan again in six hours.", difficulty);
                    return;
                }
            }
            scan.nextAttempt = clock.instant().plusSeconds(60);
        } catch (RuntimeException failure) {
            scan.nextAttempt = clock.instant().plus(Duration.ofMinutes(5));
            log.warn("Automatic {} catalog fetch failed ({}); will retry in five minutes.",
                    difficulty, failure.getClass().getSimpleName());
        }
    }

    private static class Scan {
        private int offset;
        private Instant nextAttempt = Instant.EPOCH;
    }
}
