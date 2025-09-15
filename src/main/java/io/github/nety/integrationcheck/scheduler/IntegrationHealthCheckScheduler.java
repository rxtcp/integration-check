package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.service.IntegrationHealthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@ConditionalOnProperty(
        value = "app.spring-batch.jobs.integration-health-check-job.schedule.enabled",
        havingValue = "true"
)
@Slf4j
@RequiredArgsConstructor
@Service
public class IntegrationHealthCheckScheduler {

    private final IntegrationHealthChecker healthChecker;

    @Scheduled(
            cron = "${app.spring-batch.jobs.integration-health-check-job.schedule.cron}",
            zone = "${app.spring-batch.jobs.integration-health-check-job.schedule.zone}"
    )
    public void tick() {
        healthChecker.checkHealth();
    }
}

