package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.configuration.properties.IntegrationHealthCheckJobProps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.time.Duration;
import java.util.stream.Stream;

import static io.github.nety.integrationcheck.service.IntegrationHealthCheckService.PARAM_WINDOW_START;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты для {@link IntegrationHealthCheckService}.
 */
@ExtendWith(MockitoExtension.class)
class IntegrationHealthCheckServiceTest {

    @Mock private JobLauncher jobLauncher;
    @Mock private Job integrationHealthCheckJob;
    @Mock private IntegrationHealthCheckJobProps.Schedule properties;

    private IntegrationHealthCheckService service;

    @BeforeEach
    void setUp() {
        when(properties.windowSeconds()).thenReturn(60); // безопасное окно для ассертов по времени
        service = new IntegrationHealthCheckService(jobLauncher, integrationHealthCheckJob, properties);
    }

    @Test
    void runsJobWithRoundedIdentifyingWindowParameter() throws Exception {
        when(jobLauncher.run(eq(integrationHealthCheckJob), any(JobParameters.class)))
                .thenReturn(mock(JobExecution.class));

        service.checkHealth();

        var paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(integrationHealthCheckJob), paramsCaptor.capture());

        var params = paramsCaptor.getValue();

        // 1) параметр присутствует
        Long windowStart = params.getLong(PARAM_WINDOW_START);
        assertNotNull(windowStart, "windowStart must be present");

        // 2) параметр идентифицирующий
        assertTrue(params.getParameters().get(PARAM_WINDOW_START).isIdentifying(),
                "windowStart must be marked as identifying");

        // 3) выравнивание по границе окна и принадлежность текущему окну
        long windowMillis = Duration.ofSeconds(properties.windowSeconds()).toMillis();
        long now = System.currentTimeMillis();

        assertEquals(0L, windowStart % windowMillis, "windowStart must be aligned to the window size");
        assertTrue(now - windowStart >= 0 && now - windowStart < windowMillis,
                "windowStart must fall within the current window");
    }

    @ParameterizedTest
    @MethodSource("launcherExceptionsHandled")
    void swallowsExpectedLauncherExceptions(Exception toThrow) throws Exception {
        when(jobLauncher.run(eq(integrationHealthCheckJob), any(JobParameters.class)))
                .thenThrow(toThrow);

        assertDoesNotThrow(() -> service.checkHealth());
        verify(jobLauncher).run(eq(integrationHealthCheckJob), any(JobParameters.class));
    }

    private static Stream<Exception> launcherExceptionsHandled() {
        return Stream.of(
                new JobExecutionAlreadyRunningException("already running"),
                new JobInstanceAlreadyCompleteException("already complete"),
                new JobRestartException("restart not allowed"),
                new JobParametersInvalidException("invalid parameters")
        );
    }
}