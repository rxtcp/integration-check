package io.github.rxtcp.integrationcheck.scheduler;

import io.github.rxtcp.integrationcheck.service.IntegrationHealthChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit-тест для {@link IntegrationHealthCheckScheduler}.
 * Проверяем, что вызов tick() делегирует работу доменному сервису.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationHealthCheckScheduler")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckSchedulerTest {

    @Mock
    private IntegrationHealthChecker healthChecker;

    @InjectMocks
    private IntegrationHealthCheckScheduler scheduler;

    @Test
    @DisplayName("должен вызывать healthChecker.checkHealth(), когда вызывается tick()")
    void should_invoke_checkHealth_when_tick_is_called() {
        // when
        scheduler.tick();

        // then
        // Ровно один делегированный вызов и никаких дополнительных взаимодействий
        verify(healthChecker, times(1)).checkHealth();
        verifyNoMoreInteractions(healthChecker);
    }
}