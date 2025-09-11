package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.cluster.ClusterSingletonGuard;
import io.github.nety.integrationcheck.cluster.NodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterAwareScheduler {

    private static final String JOB_NAME = "integrationHealthJob";

    private final JobLauncher jobLauncher;
    private final Job integrationHealthJob;
    private final ClusterSingletonGuard guard;

    /**
     * Запуск раз в минуту детерминированными параметрами окна (UTC)
     */
    @Scheduled(cron = "0 * * * * *")
    public void tick() throws UnknownHostException {
        String nodeId = NodeId.current();

        if (!guard.isActiveFor(JOB_NAME, nodeId)) {
            log.debug("Standby pod {}, not active for {}", nodeId, JOB_NAME);
            return;
        }
        if (!guard.tryAdvisoryLock(JOB_NAME)) {
            log.warn("Pod {} is active but lock is held, skip run", nodeId);
            return;
        }

        try {
            ZonedDateTime end = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);
            ZonedDateTime start = end.minusMinutes(1);

            JobParameters params = new JobParametersBuilder()
                    .addString("windowStart", start.toString())
                    .addString("windowEnd", end.toString())
                    .toJobParameters();

            jobLauncher.run(integrationHealthJob, params);
            log.info("Launched {} on pod {}", JOB_NAME, nodeId);

        } catch (Exception e) {
            log.error("Failed to run {}", JOB_NAME, e);
        } finally {
            guard.releaseAdvisoryLock(JOB_NAME);
        }
    }
}