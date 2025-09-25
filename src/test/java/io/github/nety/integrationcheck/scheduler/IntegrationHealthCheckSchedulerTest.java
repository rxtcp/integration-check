package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.service.IntegrationHealthChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationHealthCheckSchedulerTest {

    @Mock
    private IntegrationHealthChecker healthChecker;

    @InjectMocks
    private IntegrationHealthCheckScheduler scheduler;

    @Test
    @DisplayName("tick() вызывает checkHealth() у IntegrationHealthChecker")
    void tickInvokesCheckHealth() {
        // when
        scheduler.tick();

        // then
        verify(healthChecker).checkHealth();
        verifyNoMoreInteractions(healthChecker);
    }
}