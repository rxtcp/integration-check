package io.github.rxtcp.integrationcheck.service;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.enums.HttpMethod;
import io.github.rxtcp.integrationcheck.mapper.CheckMapper;
import io.github.rxtcp.integrationcheck.service.processor.CheckProcessor;
import io.github.rxtcp.integrationcheck.service.reader.CheckReader;
import io.github.rxtcp.integrationcheck.service.writer.CheckResultWriter;
import io.github.rxtcp.integrationcheck.service.writer.CheckWriter;
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
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Интеграционный тест транзакционного поведения CheckExecutionService.
 * <p>
 * Цель: метод execute(id) должен ОТКЛЮЧАТЬ текущую транзакцию (выполняться без активной TX),
 * даже если вызывается из @Transactional-контекста.
 */
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("CheckExecutionService — транзакционное поведение execute(..)")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckExecutionServiceTxIntegrationTest {

    private static final long CHECK_ID = 1L;
    private static final long RESULT_ID = 10L;

    // фиксированные даты, чтобы избежать флаков в тестах
    private static final LocalDateTime NOW = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    private static final LocalDateTime NEXT_RUN_AT = NOW.plusMinutes(5);

    @Autowired
    CheckExecutionService service;

    @MockitoBean
    CheckMapper checkMapper;
    @MockitoBean
    CheckReader checkReader;
    @MockitoBean
    CheckProcessor checkProcessor;
    @MockitoBean
    CheckWriter checkWriter;
    @MockitoBean
    CheckResultWriter checkResultWriter;

    // ===== Хелперы DTO/Entity (с фиксированными значениями) =====================================

    private static RestApiProfileDto profileDto() {
        return new RestApiProfileDto(
                CHECK_ID,         // checkId
                2L,               // profileId
                "https://example.org",
                HttpMethod.GET,
                5,
                null,
                null,
                200
        );
    }

    private static CheckDto checkDto() {
        return new CheckDto(
                CHECK_ID,
                "check-name",
                "desc",
                true,
                5,
                NOW.plusMinutes(1),
                CheckType.REST_API,
                profileDto()
        );
    }

    private static Check checkEntity() {
        return Check.builder()
                .id(CHECK_ID)
                .name("entity")
                .description("desc")
                .enabled(true)
                .runIntervalMin(5)
                .nextRunAt(NEXT_RUN_AT)
                .type(CheckType.REST_API)
                .build();
    }

    // ============================================================================================

    @Transactional
    @Test
    @DisplayName("должен выполняться без активной транзакции, даже если вызван из @Transactional контекста")
    void should_run_without_active_tx_even_when_called_from_transactional_context() {
        // given
        // Убеждаемся, что сам тест действительно выполняется в активной транзакции
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .as("снаружи ожидается активная транзакция (метод помечен @Transactional)")
                .isTrue();

        AtomicBoolean txActiveInsideService = new AtomicBoolean(true);

        // На первом вызове сервис читает сущность — в этот момент фиксируем флаг активности TX
        given(checkReader.findWithProfileById(CHECK_ID)).willAnswer(inv -> {
            txActiveInsideService.set(TransactionSynchronizationManager.isActualTransactionActive());
            return checkEntity();
        });

        // Старт записи истории выполнения
        given(checkResultWriter.recordProcessStart(any()))
                .willReturn(CheckResult.builder().id(RESULT_ID).build());

        // Маппинг и обработка
        given(checkMapper.toDto(any(Check.class))).willReturn(checkDto());
        given(checkProcessor.process(any(CheckDto.class)))
                .willReturn(new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok"));

        // Завершение истории и перенос nextRunAt
        given(checkResultWriter.recordProcessEnd(any(CheckResult.class), any(CheckResultDto.class)))
                .willReturn(CheckResult.builder().id(RESULT_ID).build());
        given(checkWriter.updateNextExecutionTime(any(Check.class), any()))
                .willReturn(checkEntity());

        // when
        service.execute(CHECK_ID);

        // then
        // Внутри execute(..) транзакции быть НЕ должно
        assertThat(txActiveInsideService.get())
                .as("внутри execute(..) транзакция должна быть отключена")
                .isFalse();

        // Минимальные верификации жизненного цикла
        then(checkReader).should().findWithProfileById(CHECK_ID);
        then(checkResultWriter).should().recordProcessStart(any());
        then(checkMapper).should().toDto(any(Check.class));
        then(checkProcessor).should().process(any(CheckDto.class));
        then(checkResultWriter).should().recordProcessEnd(any(CheckResult.class), any(CheckResultDto.class));
        then(checkWriter).should().updateNextExecutionTime(any(Check.class), any());
    }
}