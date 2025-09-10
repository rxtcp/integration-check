package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.service.IntegrationHealthChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.jobs.integration-health-check",
        name = "enabled",
        havingValue = "true"
)
public class IntegrationHealthCheckJob {

    private final IntegrationHealthChecker integrationHealthChecker;

    @Scheduled(
            cron = "${app.jobs.integration-health-check.cron}",
            zone = "${app.jobs.integration-health-check.zone:UTC}"
    )
    public void run() {
        integrationHealthChecker.checkHealth();
    }
}
