package io.github.rxtcp.integrationcheck.service;

import io.github.rxtcp.integrationcheck.configuration.properties.IntegrationHealthCheckJobProps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
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

import static io.github.rxtcp.integrationcheck.service.IntegrationHealthCheckService.PARAM_WINDOW_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link IntegrationHealthCheckService}.
 *
 * Что проверяем:
 * - формирование идентифицирующего параметра окна с выравниванием по границе;
 * - корректную обработку ожидаемых исключений JobLauncher.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrationHealthCheckService")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckServiceTest {

    private static final int WINDOW_SECONDS = 60; // безопасное окно для ассертов

    @Mock private JobLauncher jobLauncher;
    @Mock private Job integrationHealthCheckJob;
    @Mock
    private IntegrationHealthCheckJobProps.Schedule scheduleProps;

    private IntegrationHealthCheckService service;

    @BeforeEach
    void setUp() {
        given(scheduleProps.windowSeconds()).willReturn(WINDOW_SECONDS);
        service = new IntegrationHealthCheckService(jobLauncher, integrationHealthCheckJob, scheduleProps);
    }

    @Test
    @DisplayName("должен запускать Job с идентифицирующим параметром windowStart, выровненным по границе окна")
    void should_run_job_with_identifying_window_param_aligned_to_window_boundary() throws Exception {
        // given
        given(jobLauncher.run(eq(integrationHealthCheckJob), any(JobParameters.class)))
                .willReturn(mock(JobExecution.class));

        final long windowMillis = Duration.ofSeconds(scheduleProps.windowSeconds()).toMillis();

        // фиксируем «до» и «после» — чтобы устранить флаки на границе окна
        final long nowBefore = System.currentTimeMillis();

        // when
        service.checkHealth();

        final long nowAfter = System.currentTimeMillis();

        // then
        var paramsCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(integrationHealthCheckJob), paramsCaptor.capture());

        JobParameters params = paramsCaptor.getValue();

        // 1) параметр присутствует и помечен как идентифицирующий
        Long windowStart = params.getLong(PARAM_WINDOW_START);
        assertThat(windowStart).as("windowStart должен присутствовать").isNotNull();
        assertThat(params.getParameters().get(PARAM_WINDOW_START).isIdentifying())
                .as("windowStart должен быть идентифицирующим")
                .isTrue();

        // 2) корректное выравнивание по границе окна
        assertThat(windowStart % windowMillis)
                .as("windowStart должен быть кратен размеру окна")
                .isZero();

        // 3) принадлежность актуальному окну:
        // сервис мог округлить по времени «до» или «после» вызова, поэтому допускаем оба варианта
        long expectedStartLower = (nowBefore / windowMillis) * windowMillis;
        long expectedStartUpper = (nowAfter / windowMillis) * windowMillis;

        assertThat(windowStart)
                .as("windowStart должен соответствовать текущему окну выполнения")
                .isIn(expectedStartLower, expectedStartUpper);
    }

    @ParameterizedTest(name = "должен проглотить исключение JobLauncher: {0}")
    @MethodSource("launcherExceptionsHandled")
    void should_swallow_expected_joblauncher_exceptions(Exception toThrow) throws Exception {
        // given
        given(jobLauncher.run(eq(integrationHealthCheckJob), any(JobParameters.class)))
                .willThrow(toThrow);

        // when / then
        assertThatCode(() -> service.checkHealth()).doesNotThrowAnyException();
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