package io.github.nety.integrationcheck.configuration;

import io.github.nety.integrationcheck.configuration.properties.JobLauncherProps;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Конфигурация асинхронного запуска Spring Batch Job'ов.
 *
 * <p>Определяет:
 * <ul>
 *   <li>{@link JobLauncher} на базе {@link TaskExecutorJobLauncher} — помечен как {@link Primary}.</li>
 *   <li>{@link TaskExecutor} c настраиваемыми префиксом имени потоков, лимитом конкуренции
 *          и поддержкой виртуальных потоков.</li>
 * </ul>
 *
 * <p>Параметры берутся из {@link JobLauncherProps}:
 * <ul>
 *   <li>{@code threadNamePrefix} — префикс имени потоков исполнителя;</li>
 *   <li>{@code virtualThreadsEnabled} — включить/выключить виртуальные потоки;</li>
 *   <li>{@code concurrencyLimit} — ограничение параллельного исполнения задач.</li>
 * </ul>
 */
@Configuration
public class JobLauncherConfig {

    /**
     * Основной {@link JobLauncher}, выполняющий Job'ы асинхронно через переданный {@link TaskExecutor}.
     *
     * @param jobRepository       репозиторий Spring Batch для хранения метаданных Job'ов
     * @param jobLauncherExecutor исполнитель задач для запуска шагов/джобов
     * @return сконфигурированный асинхронный {@link JobLauncher}
     * @throws Exception если проверка свойств лаунчера завершилась неуспешно
     */
    @Bean
    @Primary
    public JobLauncher asyncJobLauncher(final JobRepository jobRepository,
                                        final TaskExecutor jobLauncherExecutor) throws Exception {
        final var jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(jobLauncherExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * Исполнитель задач для {@link JobLauncher}.
     *
     * <p>Использует {@link SimpleAsyncTaskExecutor}:
     * <ul>
     *   <li>Имена потоков имеют заданный префикс для удобной диагностики;</li>
     *   <li>Может работать на виртуальных потоках (если включено в конфигурации);</li>
     *   <li>Имеет ограничение конкурентности для сдерживания нагрузки на систему/БД.</li>
     * </ul>
     *
     * @param properties конфигурация исполнителя (см. {@link JobLauncherProps})
     * @return настроенный {@link TaskExecutor}
     */
    @Bean
    public TaskExecutor jobLauncherExecutor(final JobLauncherProps properties) {
        var executor = new SimpleAsyncTaskExecutor(properties.threadNamePrefix());
        executor.setVirtualThreads(properties.virtualThreadsEnabled());
        executor.setConcurrencyLimit(properties.concurrencyLimit());
//        executor.setTaskDecorator(new StableIndexThreadNameTaskDecorator(
//                properties.threadNamePrefix(), properties.concurrencyLimit()
//        ));
        return executor;
    }
}
