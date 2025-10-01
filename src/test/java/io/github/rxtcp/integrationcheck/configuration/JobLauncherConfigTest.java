package io.github.rxtcp.integrationcheck.configuration;

import io.github.rxtcp.integrationcheck.configuration.properties.JobLauncherProps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для конфигурации {@link JobLauncherConfig}.
 * <p>
 * Цели:
 * 1) Executor для JobLauncher настраивается с нужным префиксом, режимом виртуальных потоков и лимитом конкуренции.
 * 2) Ограничение конкуренции реально работает (execute блокируется при достижении лимита).
 * 3) Async JobLauncher собирается как {@link TaskExecutorJobLauncher} и получает переданные зависимости.
 */
@DisplayName("JobLauncherConfig")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class JobLauncherConfigTest {

    private static final long SHORT_AWAIT_MS = 250L;
    private static final long DEFAULT_AWAIT_MS = 2_000L;

    private final JobLauncherConfig config = new JobLauncherConfig();

    @Mock
    private JobLauncherProps props;

    @Test
    void creates_SimpleAsyncTaskExecutor_with_configured_prefix_virtual_threads_and_concurrency_limit() throws Exception {
        // given
        when(props.threadNamePrefix()).thenReturn("batch-exec-");
        when(props.virtualThreadsEnabled()).thenReturn(true);
        when(props.concurrencyLimit()).thenReturn(2);

        // when
        TaskExecutor taskExecutor = config.jobLauncherExecutor(props);

        // then
        assertThat(taskExecutor).isInstanceOf(SimpleAsyncTaskExecutor.class);
        SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) taskExecutor;

        // Проверяем префикс имени потока и реальное использование виртуального потока
        CountDownLatch taskRan = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        AtomicBoolean virtualFlag = new AtomicBoolean(false);

        executor.execute(() -> {
            threadName.set(Thread.currentThread().getName());
            virtualFlag.set(Thread.currentThread().isVirtual());
            taskRan.countDown();
        });

        assertThat(taskRan.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(threadName.get()).startsWith("batch-exec-");
        assertThat(virtualFlag.get()).isTrue();

        // Лимит конкуренции выставлен
        assertThat(executor.getConcurrencyLimit()).isEqualTo(2);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
        // защита от зависаний в случае регрессии
    void throttles_execution_when_concurrency_limit_is_one() throws Exception {
        // given
        when(props.threadNamePrefix()).thenReturn("throttle-");
        when(props.virtualThreadsEnabled()).thenReturn(false);
        when(props.concurrencyLimit()).thenReturn(1);

        SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) config.jobLauncherExecutor(props);

        CountDownLatch allowFirstTaskToFinish = new CountDownLatch(1);
        CountDownLatch secondTaskStarted = new CountDownLatch(1);
        CountDownLatch secondSubmitReturned = new CountDownLatch(1);

        // Первая "долгая" задача занимает единственный слот
        executor.execute(() -> {
            try {
                allowFirstTaskToFinish.await(750, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        // Сабмит второй задачи в отдельном потоке.
        // Если троттлинг работает, этот поток заблокируется внутри execute() до освобождения слота.
        Thread submitterThread = new Thread(() -> {
            executor.execute(() -> secondTaskStarted.countDown());
            // Сюда поток попадёт только когда execute() вернётся (после освобождения слота)
            secondSubmitReturned.countDown();
        }, "submitter");
        submitterThread.start();

        // Пока первая задача висит — submit второй должен быть заблокирован
        assertThat(secondSubmitReturned.await(SHORT_AWAIT_MS, TimeUnit.MILLISECONDS)).isFalse();

        // Освобождаем слот
        allowFirstTaskToFinish.countDown();

        // Теперь submit должен завершиться, а сама задача — стартовать
        assertThat(secondSubmitReturned.await(DEFAULT_AWAIT_MS, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(secondTaskStarted.await(DEFAULT_AWAIT_MS, TimeUnit.MILLISECONDS)).isTrue();

        // Гарантируем отсутствие "висящего" системного потока сабмиттера
        submitterThread.join(DEFAULT_AWAIT_MS);
    }

    @Test
    void builds_TaskExecutorJobLauncher_with_injected_dependencies() throws Exception {
        // given: подменяем зависимости моками
        var jobRepository = mock(org.springframework.batch.core.repository.JobRepository.class);
        var taskExecutor = mock(TaskExecutor.class);

        // when
        JobLauncher launcher = config.asyncJobLauncher(jobRepository, taskExecutor);

        // then
        assertThat(launcher).isInstanceOf(TaskExecutorJobLauncher.class);

        // Проверяем, что внутрь реально попали наши зависимости
        Object actualRepo = ReflectionTestUtils.getField(launcher, "jobRepository");
        Object actualExec = ReflectionTestUtils.getField(launcher, "taskExecutor");

        assertThat(actualRepo).isSameAs(jobRepository);
        assertThat(actualExec).isSameAs(taskExecutor);
    }
}