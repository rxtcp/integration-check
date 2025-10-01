package io.github.rxtcp.integrationcheck.configuration;

import io.github.rxtcp.integrationcheck.service.CheckExecution;
import io.github.rxtcp.integrationcheck.service.reader.CheckReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Интеграционный тест для конфигурации джоба интеграционных проверок.
 * <p>
 * Цели:
 * 1) Джоб успешно завершается со статусом COMPLETED.
 * 2) В каждую worker-партицию пробрасывается корректный {@code checkId}.
 * 3) Доменный сервис {@link CheckExecution} вызывается для каждого ID из {@link CheckReader}.
 * <p>
 * Примечание: используем @SpringBatchTest + JobLauncherTestUtils для запуска job в тестовом контексте.
 */
@SpringBatchTest
@ActiveProfiles("test")
@ConfigurationPropertiesScan
@SpringBootTest(classes = IntegrationHealthCheckJobIntegrationTest.ITApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("IntegrationHealthCheckJob: интеграционный сценарий")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckJobIntegrationTest {

    // === Инжекция тестовых утилит и вспомогательных бинов ===
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private JobExplorer jobExplorer;
    @Autowired
    private ExecutedIdsCollector executedIdsCollector;

    @Test
    void should_run_all_partitions_and_pass_checkId() throws Exception {
        // when: запускаем job
        final JobExecution execution = jobLauncherTestUtils.launchJob();

        // then: дожидаемся завершения job (без busy-wait, через Awaitility)
        await()
                .pollInterval(Duration.ofMillis(100))
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    final JobExecution je = jobExplorer.getJobExecution(execution.getId());
                    return je != null && !je.isRunning();
                });

        final JobExecution finished = jobExplorer.getJobExecution(execution.getId());
        assertThat(finished).isNotNull();
        assertThat(finished.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // доменный сервис вызван для всех ID
        assertThat(executedIdsCollector.get())
                .as("должны быть выполнены все ID из CheckReader")
                .containsExactlyInAnyOrder(1L, 2L, 5L);

        // в worker-партициях корректно проброшен checkId
        final var workerStepIds = finished.getStepExecutions().stream()
                .filter(se -> se.getStepName().startsWith("workerStep"))
                .filter(se -> se.getExecutionContext().containsKey("checkId"))
                .map(se -> se.getExecutionContext().getLong("checkId"))
                .collect(Collectors.toSet());

        assertThat(workerStepIds)
                .as("каждая партиция должна содержать свой checkId")
                .containsExactlyInAnyOrder(1L, 2L, 5L);
    }

    @SpringBootApplication
    @Import({IntegrationHealthCheckJobConfig.class, TestConfig.class})
    static class ITApp {
    }

    /**
     * Утилитарный бин, чтобы безопасно забирать выполненные ID за пределами лямбды.
     */
    static class ExecutedIdsCollector {
        private final ConcurrentLinkedQueue<Long> ids;

        ExecutedIdsCollector(ConcurrentLinkedQueue<Long> ids) {
            this.ids = ids;
        }

        Set<Long> get() {
            return Set.copyOf(ids);
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {

        private final ConcurrentLinkedQueue<Long> executedIds = new ConcurrentLinkedQueue<>();

        @Bean
        CheckReader checkReader() {
            // источник ID для партиционирования
            final var reader = mock(CheckReader.class);
            when(reader.findDueIds()).thenReturn(List.of(1L, 2L, 5L));
            return reader;
        }

        @Bean
        CheckExecution checkExecution() {
            // простая реализация доменного сервиса — аккумулируем выполненные ID
            return executedIds::add;
        }

        @Bean
        ExecutedIdsCollector executedIdsCollector() {
            return new ExecutedIdsCollector(executedIds);
        }
    }
}