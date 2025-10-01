package io.github.rxtcp.integrationcheck.configuration;

import io.github.rxtcp.integrationcheck.service.CheckExecution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для {@link IntegrationHealthCheckJobConfig#checkTasklet(Long, CheckExecution)}.
 * <p>
 * Идея набора:
 * 1) Tasklet должен вызывать доменный сервис с переданным идентификатором и завершаться статусом FINISHED.
 * 2) В случае исключения сервиса — пробрасывать его наверх (Batch сам зафейлит шаг).
 */
@DisplayName("IntegrationHealthCheckJobConfig.checkTasklet(...)")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class IntegrationHealthCheckJobConfigTaskletTest {

    @Mock
    private CheckExecution exec;

    @ParameterizedTest(name = "[{index}] checkId={0}")
    @ValueSource(longs = {1L, 99L, 100_500L})
    void should_invoke_domain_service_and_finish(long checkId) throws Exception {
        // given
        final var cfg = new IntegrationHealthCheckJobConfig();
        final Tasklet tasklet = cfg.checkTasklet(checkId, exec);

        // when
        final RepeatStatus status = tasklet.execute(
                mock(StepContribution.class),
                mock(ChunkContext.class)
        );

        // then
        verify(exec).execute(checkId);          // вызов доменного сервиса с нужным ID
        verifyNoMoreInteractions(exec);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED); // tasklet завершает шаг
    }

    @Test
    void should_propagate_exception_from_domain_service() throws Exception {
        // given
        final long checkId = 42L;
        doThrow(new IllegalStateException("boom")).when(exec).execute(checkId);

        final var cfg = new IntegrationHealthCheckJobConfig();
        final Tasklet tasklet = cfg.checkTasklet(checkId, exec);

        // when / then
        assertThatThrownBy(() -> tasklet.execute(
                mock(StepContribution.class),
                mock(ChunkContext.class)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        verify(exec).execute(checkId);
        verifyNoMoreInteractions(exec);
    }
}