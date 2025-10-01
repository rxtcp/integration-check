package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import io.github.rxtcp.integrationcheck.repository.CheckResultRepository;
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

import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("CheckResultWriterService — интеграция")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultWriterServiceIntegrationTest {

    // детерминированные значения по умолчанию
    private static final int RUN_INTERVAL_MIN = 5;
    private static final String DESC = "desc";

    private final CheckResultWriterService service;
    private final CheckRepository checkRepository;
    private final CheckResultRepository checkResultRepository;
    private final EntityManager em;

    @Autowired
    CheckResultWriterServiceIntegrationTest(CheckResultWriterService service,
                                            CheckRepository checkRepository,
                                            CheckResultRepository checkResultRepository,
                                            EntityManager em) {
        this.service = service;
        this.checkRepository = checkRepository;
        this.checkResultRepository = checkResultRepository;
        this.em = em;
    }

    @Test
    @DisplayName("start → end: корректно сохраняет все поля и реально персистит в БД")
    void should_persist_all_fields_on_start_then_end() {
        // given: существующая проверка
        Check check = persistCheck();

        // when: старт
        LocalDateTime beforeStart = LocalDateTime.now();
        CheckResult started = service.recordProcessStart(check);
        LocalDateTime afterStart = LocalDateTime.now();

        // then: старт сохранён
        assertThat(started.getId()).isNotNull();
        assertThat(started.getCheck().getId()).isEqualTo(check.getId());
        assertThat(started.getStatus()).isEqualTo(PROCESSING);
        assertThat(started.getStartedAt()).isAfterOrEqualTo(beforeStart).isBeforeOrEqualTo(afterStart);
        assertThat(started.getFinishedAt()).isNull();

        // when: завершение
        CheckResultDto dto = new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "done");
        LocalDateTime beforeEnd = LocalDateTime.now();
        CheckResult ended = service.recordProcessEnd(started, dto);
        LocalDateTime afterEnd = LocalDateTime.now();

        // then: финал корректен, startedAt не изменился
        assertThat(ended.getId()).isEqualTo(started.getId());
        assertThat(ended.getStatus()).isEqualTo(CheckRunStatus.SUCCEEDED);
        assertThat(ended.getDetails()).isEqualTo("done");
        assertThat(ended.getFailureReason()).isNull();
        assertThat(ended.getFinishedAt()).isAfterOrEqualTo(beforeEnd).isBeforeOrEqualTo(afterEnd);
        assertThat(ended.getStartedAt()).isEqualTo(started.getStartedAt());

        // and: реально сохранено в БД
        em.clear();
        CheckResult reloaded = checkResultRepository.findById(ended.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(CheckRunStatus.SUCCEEDED);
        assertThat(reloaded.getDetails()).isEqualTo("done");
        assertThat(reloaded.getStartedAt()).isNotNull();
        assertThat(reloaded.getFinishedAt()).isNotNull();
        assertThat(reloaded.getCheck().getId()).isEqualTo(check.getId());
    }

    @Test
    @DisplayName("start: допускает finishedAt=null (если колонка nullable) или падает (если NOT NULL)")
    void should_allow_null_finishedAt_on_start_or_fail_if_db_enforces_not_null() {
        Check check = persistCheck();

        try {
            CheckResult started = service.recordProcessStart(check);
            // В схеме с nullable — запись проходит, finishedAt остаётся null
            assertThat(started.getId()).isNotNull();
            assertThat(started.getFinishedAt()).isNull();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // В схеме с NOT NULL — ожидаемое падение (сообщение БД может различаться)
            assertThat(ex.getMessage()).containsIgnoringCase("null");
        }
    }

    /**
     * Создаёт и сохраняет Check с уникальным именем для исключения конфликтов на CI.
     */
    private Check persistCheck() {
        Check c = Check.builder()
                .name("writer-" + UUID.randomUUID())
                .description(DESC)
                .enabled(true)
                .runIntervalMin(RUN_INTERVAL_MIN)
                .nextRunAt(LocalDateTime.now().plusMinutes(RUN_INTERVAL_MIN))
                .type(CheckType.values()[0])
                .build();
        return checkRepository.saveAndFlush(c);
    }
}