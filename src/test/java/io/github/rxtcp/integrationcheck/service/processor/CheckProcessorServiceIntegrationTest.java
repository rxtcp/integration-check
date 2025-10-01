package io.github.rxtcp.integrationcheck.service.processor;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckProfileDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.domain.CheckRunStatus;
import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import io.github.rxtcp.integrationcheck.service.processor.strategy.CheckStrategy;
import io.github.rxtcp.integrationcheck.service.processor.strategy.CheckStrategyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("CheckProcessorService (integration)")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckProcessorServiceIntegrationTest {

    private static final long CHECK_ID = 100L;
    private static final long PROFILE_ID = 2L;
    private static final String URL = "https://example.org";
    private static final int TIMEOUT_SEC = 10;
    private static final int RUN_INTERVAL_MIN = 5;

    @Autowired
    private CheckProcessorService service;

    // Мокаем фабрику и стратегию — проверяем только транзакционную семантику сервиса.
    @MockitoBean
    private CheckStrategyFactory strategyFactory;

    @MockitoBean
    private CheckStrategy strategy;

    /**
     * Строит валидный входной DTO с заданным типом проверки.
     */
    private static CheckDto newCheckDto(CheckType type) {
        CheckProfileDto profile = new RestApiProfileDto(
                CHECK_ID, PROFILE_ID, URL, HttpMethod.GET, TIMEOUT_SEC, null, null, 200
        );
        return new CheckDto(
                CHECK_ID,
                "n",
                "d",
                true,
                RUN_INTERVAL_MIN,
                LocalDateTime.now().plusMinutes(1),
                type,
                profile
        );
    }

    @Test
    @Transactional
    @DisplayName("process(): при вызове из активной TX должен выполняться БЕЗ транзакции (NOT_SUPPORTED)")
    void should_suspend_active_transaction_and_run_strategy_without_tx_when_called_from_transactional_context() {
        // given
        CheckDto input = newCheckDto(CheckType.REST_API);
        AtomicBoolean txActiveInsideStrategy = new AtomicBoolean(true);

        // Убедимся, что тест действительно выполняется в активной транзакции
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();

        given(strategyFactory.getStrategy(input.type())).willReturn(strategy);
        given(strategy.execute(any())).willAnswer(invocation -> {
            // Фиксируем состояние транзакции в момент вызова стратегии
            txActiveInsideStrategy.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok");
        });

        // when
        CheckResultDto result = service.process(input);

        // then
        then(strategyFactory).should().getStrategy(input.type());
        then(strategy).should().execute(input);

        // NOT_SUPPORTED должен приостановить внешнюю TX
        assertThat(txActiveInsideStrategy.get()).isFalse();
        assertThat(result.status()).isEqualTo(CheckRunStatus.SUCCEEDED);
    }

    // ===== Вспомогательное =====

    @Test
    @DisplayName("process(): при прямом вызове также должен выполняться без активной транзакции")
    void should_run_without_transaction_when_called_directly() {
        // given
        CheckDto input = newCheckDto(CheckType.REST_API);
        AtomicBoolean txActiveInsideStrategy = new AtomicBoolean(true);

        given(strategyFactory.getStrategy(input.type())).willReturn(strategy);
        given(strategy.execute(any())).willAnswer(invocation -> {
            txActiveInsideStrategy.set(TransactionSynchronizationManager.isActualTransactionActive());
            return new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "direct");
        });

        // when
        CheckResultDto result = service.process(input);

        // then
        then(strategyFactory).should().getStrategy(input.type());
        then(strategy).should().execute(input);

        assertThat(txActiveInsideStrategy.get()).isFalse();
        assertThat(result.details()).isEqualTo("direct");
    }
}