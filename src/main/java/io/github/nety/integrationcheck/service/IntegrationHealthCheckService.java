package io.github.nety.integrationcheck.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class IntegrationHealthCheckService implements IntegrationHealthChecker {

    private final CheckReader checkReader;
    private final CheckProcessor checkProcessor;
    private final CheckResultWriter checkResultWriter;

    @Override
    public void checkHealth() {
        checkReader.readChecks().forEach(check -> {
            checkResultWriter.saveResult(
                    checkProcessor.process(check)
            );
        });
    }
}
