package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.repository.CheckResultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.same;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckResultWriterService — запись начала/завершения выполнения проверки")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultWriterServiceUnitTest {

    // --- Константы для читаемости и детерминированности ---------------------------------------
    private static final long CHECK_ID = 100L;
    private static final long RESULT_ID = 1L;
    private static final LocalDateTime FIXED_STARTED_AT = LocalDateTime.of(2025, 1, 2, 10, 0, 0);

    @Mock
    CheckResultRepository checkResultRepository;

    @InjectMocks
    CheckResultWriterService service;

    @Test
    @DisplayName("должен проставить startedAt и статус PROCESSING, сохранить, вернуть сущность с id")
    void should_set_startedAt_and_processing_then_save_and_return_entity_with_id() {
        // given
        Check check = new Check();
        check.setId(CHECK_ID);

        // Репозиторий возвращает ту же сущность с присвоенным id
        given(checkResultRepository.save(any(CheckResult.class)))
                .willAnswer(inv -> {
                    CheckResult arg = inv.getArgument(0, CheckResult.class);
                    arg.setId(RESULT_ID);
                    return arg;
                });

        // Фиксируем временные рамки, в которые должен попасть startedAt
        LocalDateTime before = LocalDateTime.now();

        // when
        CheckResult saved = service.recordProcessStart(check);

        LocalDateTime after = LocalDateTime.now();

        // then
        ArgumentCaptor<CheckResult> captor = ArgumentCaptor.forClass(CheckResult.class);
        then(checkResultRepository).should().save(captor.capture());
        then(checkResultRepository).shouldHaveNoMoreInteractions();

        CheckResult toSave = captor.getValue();
        // Проверяем, что в save ушли корректные данные
        assertThat(toSave.getCheck()).isSameAs(check);
        assertThat(toSave.getStatus()).isEqualTo(PROCESSING);
        assertThat(toSave.getStartedAt()).isNotNull();
        assertThat(toSave.getFinishedAt()).isNull();

        // Возвращённая сущность содержит присвоенный репозиторием id и валидные поля
        assertThat(saved.getId()).isEqualTo(RESULT_ID);
        assertThat(saved.getCheck()).isSameAs(check);
        assertThat(saved.getStatus()).isEqualTo(PROCESSING);
        assertThat(saved.getStartedAt()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        assertThat(saved.getFinishedAt()).isNull();
    }

    @Test
    @DisplayName("должен проставить finishedAt и данные из DTO, сохранить и не изменять startedAt")
    void should_set_finishedAt_and_copy_dto_then_save_and_keep_startedAt_intact() {
        // given
        Check check = new Check();
        check.setId(10L);

        CheckResult current = CheckResult.builder()
                .id(5L)
                .check(check)
                .startedAt(FIXED_STARTED_AT) // фиксируем старт для стабильной проверки
                .status(PROCESSING)
                .details("prev")
                .build();

        CheckResultDto dto = new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok-details");

        given(checkResultRepository.save(any(CheckResult.class)))
                .willAnswer(inv -> inv.getArgument(0, CheckResult.class));

        LocalDateTime before = LocalDateTime.now();

        // when
        CheckResult updated = service.recordProcessEnd(current, dto);

        LocalDateTime after = LocalDateTime.now();

        // then
        then(checkResultRepository).should().save(same(current));
        then(checkResultRepository).shouldHaveNoMoreInteractions();

        assertThat(updated.getId()).isEqualTo(5L);
        assertThat(updated.getCheck()).isSameAs(check);
        assertThat(updated.getStatus()).isEqualTo(CheckRunStatus.SUCCEEDED);
        assertThat(updated.getFailureReason()).isNull();
        assertThat(updated.getDetails()).isEqualTo("ok-details");

        // finishedAt выставлен «сейчас», попадает в рамки [before, after]
        assertThat(updated.getFinishedAt()).isNotNull();
        assertThat(updated.getFinishedAt()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);

        // startedAt не изменился и не позже finishedAt
        assertThat(updated.getStartedAt()).isEqualTo(FIXED_STARTED_AT);
        assertThat(updated.getStartedAt()).isBeforeOrEqualTo(updated.getFinishedAt());
    }
}