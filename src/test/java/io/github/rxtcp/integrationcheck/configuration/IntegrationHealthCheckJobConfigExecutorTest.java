package io.github.rxtcp.integrationcheck.configuration;

import io.github.rxtcp.integrationcheck.configuration.properties.IntegrationHealthCheckJobProps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("IntegrationHealthCheckJobConfig.partitionExecutor(...)")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class IntegrationHealthCheckJobConfigExecutorTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void partitionExecutor_should_apply_prefix_virtual_threads_and_concurrency_limit() throws Exception {
        // given
        final var props = mock(IntegrationHealthCheckJobProps.WorkerStepProps.class);
        final int limit = 3;
        when(props.threadNamePrefix()).thenReturn("hc-");
        when(props.concurrencyLimit()).thenReturn(limit);
        when(props.virtualThreadsEnabled()).thenReturn(true);

        final var cfg = new IntegrationHealthCheckJobConfig();
        final TaskExecutor executor = cfg.partitionExecutor(props);

        // Координация и метрики
        final CountDownLatch firstWaveStarted = new CountDownLatch(limit);
        final CountDownLatch releaseGate = new CountDownLatch(1);
        final CountDownLatch allDone = new CountDownLatch(limit + 1); // +1 — «дополнительная» задача

        final AtomicInteger inFlight = new AtomicInteger(0);
        final AtomicInteger maxInFlight = new AtomicInteger(0);

        final List<String> threadNames = Collections.synchronizedList(new ArrayList<>());
        final List<Boolean> isVirtualFlags = Collections.synchronizedList(new ArrayList<>());

        final Runnable blockingTask = () -> {
            threadNames.add(Thread.currentThread().getName());
            isVirtualFlags.add(Thread.currentThread().isVirtual());

            final int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            firstWaveStarted.countDown();

            try {
                // Задачи первой волны удерживаются, чтобы не освободить «слоты»
                releaseGate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                inFlight.decrementAndGet();
                allDone.countDown();
            }
        };

        // when: пускаем ровно limit задач
        for (int i = 0; i < limit; i++) {
            executor.execute(blockingTask);
        }

        // Убедимся, что первая волна действительно стартовала
        assertThat(firstWaveStarted.await(2, TimeUnit.SECONDS))
                .as("первая волна задач должна стартовать (size = %s)", limit)
                .isTrue();

        // Попытка запланировать дополнительную задачу: она не должна стартовать, пока слоты заняты
        final CountDownLatch extraStarted = new CountDownLatch(1);
        final Runnable extraTask = () -> {
            threadNames.add(Thread.currentThread().getName());
            isVirtualFlags.add(Thread.currentThread().isVirtual());
            final int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            extraStarted.countDown();
            inFlight.decrementAndGet();
            allDone.countDown();
        };

        // Запускаем отправку дополнительной задачи из фонового потока — если лимит достигнут,
        // этот поток «повиснет» внутри execute(...) до освобождения слота.
        Thread submitter = new Thread(() -> executor.execute(extraTask), "submitter");
        submitter.start();

        // then: доп. задача НЕ должна стартовать, пока слоты заняты
        assertThat(extraStarted.await(300, TimeUnit.MILLISECONDS))
                .as("дополнительная задача не должна стартовать при достигнутом лимите")
                .isFalse();

        // Лимит конкуренции соблюдён (ровно limit активных задач)
        assertThat(maxInFlight.get())
                .as("максимум параллельных задач")
                .isEqualTo(limit);

        // Префикс имени потоков
        assertThat(threadNames)
                .as("собраны имена потоков")
                .isNotEmpty();
        threadNames.forEach(name -> assertThat(name).startsWith("hc-"));

        // Виртуальные потоки
        assertThat(isVirtualFlags)
                .as("собраны флаги виртуальности")
                .isNotEmpty();
        assertThat(isVirtualFlags).allMatch(Boolean::booleanValue);

        // Разблокируем первую волну и дожидаемся завершения всех задач (включая дополнительную)
        releaseGate.countDown();
        assertThat(allDone.await(8, TimeUnit.SECONDS))
                .as("все задачи должны завершиться")
                .isTrue();

        // verify
        verify(props).threadNamePrefix();
        verify(props).concurrencyLimit();
        verify(props).virtualThreadsEnabled();
        verifyNoMoreInteractions(props);
    }
}