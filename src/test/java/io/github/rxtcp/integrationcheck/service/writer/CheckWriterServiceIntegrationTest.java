package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("CheckWriterService — интеграция с БД")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckWriterServiceIntegrationTest {

    // --- Константы для детерминированности теста ---
    private static final int INTERVAL_MINUTES = 30;
    private static final LocalDateTime INITIAL_NEXT_RUN_AT = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime FINISHED_AT = LocalDateTime.of(2025, 1, 2, 12, 0);

    private final CheckWriterService service;
    private final CheckRepository checkRepository;
    private final EntityManager em;

    @Autowired
    CheckWriterServiceIntegrationTest(CheckWriterService service,
                                      CheckRepository checkRepository,
                                      EntityManager em) {
        this.service = service;
        this.checkRepository = checkRepository;
        this.em = em;
    }

    @Test
    @DisplayName("должен вычислять nextRunAt = finishedAt + interval, сохранять и реально персистить в БД")
    void should_persist_computed_nextRunAt() {
        // given
        Check check = persistCheck(INTERVAL_MINUTES);
        CheckResult result = CheckResult.builder()
                .finishedAt(FINISHED_AT)
                .build();

        // when
        Check returned = service.updateNextExecutionTime(check, result);

        // then: сервис вернул обновлённую проверку с ожидаемым nextRunAt
        assertThat(returned.getId()).isEqualTo(check.getId());
        assertThat(returned.getNextRunAt()).isEqualTo(FINISHED_AT.plusMinutes(INTERVAL_MINUTES));

        // and: состояние действительно сохранено в БД
        em.clear(); // сбрасываем контекст, чтобы читать «как из БД»
        Check reloaded = checkRepository.findById(check.getId()).orElseThrow();
        assertThat(reloaded.getNextRunAt()).isEqualTo(FINISHED_AT.plusMinutes(INTERVAL_MINUTES));
    }

    /**
     * Создаёт и сохраняет Check с заданным интервалом. Уникальность имени исключает конфликты на CI.
     */
    private Check persistCheck(int intervalMin) {
        Check check = Check.builder()
                .name("writer-it-" + UUID.randomUUID())
                .description("desc")
                .enabled(true)
                .runIntervalMin(intervalMin)
                .nextRunAt(INITIAL_NEXT_RUN_AT)
                .type(CheckType.values()[0])
                .build();
        return checkRepository.saveAndFlush(check);
    }
}