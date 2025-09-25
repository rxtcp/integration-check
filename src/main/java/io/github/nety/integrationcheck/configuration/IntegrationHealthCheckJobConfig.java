package io.github.nety.integrationcheck.configuration;

import io.github.nety.integrationcheck.configuration.properties.IntegrationHealthCheckJobProps;
import io.github.nety.integrationcheck.service.CheckExecution;
import io.github.nety.integrationcheck.service.reader.CheckReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.HashMap;

/**
 * Конфигурация пакетной задачи проверки интеграций.
 *
 * <p>Модель: master/worker c партиционированием по идентификатору проверки.
 * Master-ступень генерирует партиции на основе списка просроченных/должных к запуску проверок,
 * worker-ступень исполняет каждую проверку параллельно с заданным лимитом.</p>
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class IntegrationHealthCheckJobConfig {

    /**
     * Имя job для оперативной поддержки и трекинга.
     */
    private static final String JOB_NAME = "integrationHealthCheckJob";
    private static final String MASTER_STEP = "masterStep";
    private static final String WORKER_STEP = "workerStep";
    private static final String PARTITION_PREFIX = "check-";
    /**
     * Ключ параметра партиции: идентификатор проверки.
     */
    private static final String PARAM_CHECK_ID = "checkId";

    /**
     * Определение пакетной задачи.
     *
     * @param masterStep     партиционированная ступень
     * @param jobRepository  репозиторий метаданных Spring Batch
     */
    @Bean
    public Job integrationHealthCheckJob(Step masterStep, JobRepository jobRepository) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(masterStep)
                .build();
    }

    /**
     * Master-ступень: разбивает работу на партиции и делегирует выполнение worker-ступени.
     *
     * @param workerStep         исполняемая ступень для каждой партиции
     * @param checkIdPartitioner стратегія партиционирования по идентификаторам проверок
     * @param partitionExecutor  исполнитель для параллельного запуска партиций
     * @param jobRepository      репозиторий метаданных
     */
    @Bean
    public Step masterStep(
            Step workerStep,
            Partitioner checkIdPartitioner,
            TaskExecutor partitionExecutor,
            JobRepository jobRepository
    ) {
        return new StepBuilder(MASTER_STEP, jobRepository)
                .partitioner(WORKER_STEP, checkIdPartitioner)
                .step(workerStep)
                .taskExecutor(partitionExecutor)
                .build();
    }

    /**
     * Партиционирование по {@code checkId}: на каждую проверку формируется отдельный {@link ExecutionContext}.
     *
     * <p>Источник идентификаторов — {@link CheckReader#findDueIds()}.</p>
     */
    @Bean
    public Partitioner checkIdPartitioner(CheckReader checkReader) {
        return gridSize -> {
            var partitionContexts = new HashMap<String, ExecutionContext>();
            checkReader.findDueIds().forEach(checkId -> {
                var ctx = new ExecutionContext();
                ctx.putLong(PARAM_CHECK_ID, checkId);
                partitionContexts.put(PARTITION_PREFIX + checkId, ctx);
            });
            return partitionContexts;
        };
    }

    /**
     * Worker-ступень: выполняет отдельную проверку из контекста партиции.
     *
     * <p>Используется {@link ResourcelessTransactionManager}, т.к. само исполнение проверки
     * не требует транзакций БД (метаданные job управляются {@link JobRepository}).</p>
     */
    @Bean
    public Step workerStep(JobRepository jobRepository, Tasklet checkTasklet) {
        return new StepBuilder(WORKER_STEP, jobRepository)
                .tasklet(checkTasklet, new ResourcelessTransactionManager())
                .build();
    }

    /**
     * Исполнитель партиций.
     *
     * <p>Поддерживает виртуальные потоки и ограничение конкуренции.
     * Имя потока задаётся префиксом из настроек для удобства трассировки.</p>
     */

    @Bean
    public TaskExecutor partitionExecutor(final IntegrationHealthCheckJobProps props) {
        var executor = new SimpleAsyncTaskExecutor(props.threadNamePrefix());
        executor.setVirtualThreads(props.virtualThreadsEnabled());
        executor.setConcurrencyLimit(props.concurrencyLimit());
//        executor.setTaskDecorator(new StableIndexThreadNameTaskDecorator(
//                props.threadNamePrefix(), props.concurrencyLimit()
//        ));
        return executor;
    }

    /**
     * Tasklet, запускающий исполнение одной проверки.
     *
     * @param checkId        идентификатор проверки, внедряется из {@code stepExecutionContext}
     * @param checkExecution сервис доменного исполнения проверки
     */
    @StepScope
    @Bean
    public Tasklet checkTasklet(
            @Value("#{stepExecutionContext['" + PARAM_CHECK_ID + "']}") Long checkId,
            CheckExecution checkExecution
    ) {
        return (contribution, chunkContext) -> {
            checkExecution.execute(checkId);
            return RepeatStatus.FINISHED;
        };
    }
}