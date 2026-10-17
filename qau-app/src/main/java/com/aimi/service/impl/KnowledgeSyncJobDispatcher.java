package com.aimi.service.impl;

import com.aimi.entity.KnowledgeSyncJobEntity;
import com.aimi.mapper.KnowledgeSyncJobMapper;
import com.aimi.service.KnowledgeDocumentParseService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Polls the database queue and hands claimed jobs to a bounded executor. */
@Slf4j
@Service
public class KnowledgeSyncJobDispatcher {

    private final KnowledgeSyncJobMapper jobMapper;
    private final KnowledgeDocumentParseService parseService;
    private final PlatformTransactionManager transactionManager;
    private final Executor parseExecutor;

    @Value("${app.knowledge.sync.batch-size:5}")
    private int batchSize;
    @Value("${app.knowledge.sync.lease-seconds:600}")
    private long leaseSeconds;
    private final String workerId = UUID.randomUUID().toString();

    public KnowledgeSyncJobDispatcher(KnowledgeSyncJobMapper jobMapper,
                                      KnowledgeDocumentParseService parseService,
                                      PlatformTransactionManager transactionManager,
                                      @Qualifier("knowledgeParseExecutor") Executor parseExecutor) {
        this.jobMapper = jobMapper;
        this.parseService = parseService;
        this.transactionManager = transactionManager;
        this.parseExecutor = parseExecutor;
    }

    @Scheduled(fixedDelayString = "${app.knowledge.sync.poll-interval-ms:2000}", initialDelayString = "${app.knowledge.sync.initial-delay-ms:1000}")
    public void dispatch() {
        int recovered = jobMapper.recoverExpired();
        if (recovered > 0) {
            log.info("Recovered {} expired knowledge sync jobs", recovered);
        }
        List<KnowledgeSyncJobEntity> jobs = claimJobs();
        for (KnowledgeSyncJobEntity job : jobs) {
            try {
                parseExecutor.execute(() -> parseService.parse(job));
            } catch (RejectedExecutionException e) {
                // The queue is durable; put the claim back into retry state instead of dropping it.
                jobMapper.markFailure(job.getId(), workerId, "failed", false,
                        LocalDateTime.now().plusMinutes(1), e.getClass().getSimpleName(), "解析线程池已满");
            }
        }
    }

    private List<KnowledgeSyncJobEntity> claimJobs() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            List<KnowledgeSyncJobEntity> selected = jobMapper.selectClaimable(Math.max(1, batchSize));
            selected.removeIf(job -> jobMapper.claim(job.getId(), workerId, leaseSeconds) != 1);
            for (KnowledgeSyncJobEntity job : selected) {
                job.setStatus("running");
                job.setWorkerId(workerId);
                job.setAttempts((job.getAttempts() == null ? 0 : job.getAttempts()) + 1);
                job.setLeaseUntil(LocalDateTime.now().plusSeconds(leaseSeconds));
            }
            return selected;
        });
    }
}
