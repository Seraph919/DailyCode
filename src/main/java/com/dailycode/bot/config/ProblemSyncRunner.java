package com.dailycode.bot.config;

import com.dailycode.bot.application.problem.ProblemSyncService;
import com.dailycode.bot.application.problem.dto.ProblemSyncResultDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("problem-sync")
public class ProblemSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemSyncRunner.class);

    private final ProblemSyncService syncService;
    private final String slug;
    private final boolean includePaid;

    public ProblemSyncRunner(ProblemSyncService syncService,
                             @Value("${problem-sync.slug}") String slug,
                             @Value("${problem-sync.include-paid:false}") boolean includePaid) {
        this.syncService = syncService;
        this.slug = slug;
        this.includePaid = includePaid;
    }

    @Override
    public void run(ApplicationArguments args) {
        ProblemSyncResultDto result = syncService.sync(slug, includePaid);
        log.info("Problem sync: status={}, slug={}, problemId={}.",
                result.getStatus(), result.getTitleSlug(), result.getProblemId());
    }
}
