package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckWriterService — обновление nextRunAt и сохранение")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckWriterServiceUnitTest {

    // ---- Константы для стабильности тестов ----------------------------------------------------
    private static final long CHECK_ID = 10L;
    private static final int RUN_INTERVAL_MIN = 15;

    private static final LocalDateTime INITIAL_NEXT_RUN_AT = LocalDateTime.of(2025, 1, 1, 0, 0);
    private static final LocalDateTime FINISHED_AT = LocalDateTime.of(2025, 1, 2, 3, 4, 5);

    @Mock
    CheckRepository checkRepository;

    @InjectMocks
    CheckWriterService service;

    @Test
    @DisplayName("должен выставлять nextRunAt=finishedAt+interval, вызывать save и возвращать результат репозитория")
    void should_set_nextRunAt_and_save_and_return_repo_result() {
        // given
        Check check = Check.builder()
                .id(CHECK_ID)
                .name("name")
                .description("desc")
                .enabled(true)
                .runIntervalMin(RUN_INTERVAL_MIN)
                .nextRunAt(INITIAL_NEXT_RUN_AT)
                .type(CheckType.values()[0])
                .build();

        CheckResult result = CheckResult.builder()
                .finishedAt(FINISHED_AT)
                .build();

        // Репозиторий вернёт «другой» экземпляр — проверим, что сервис возвращает именно его
        Check repoReturn = new Check();
        repoReturn.setId(CHECK_ID);
        given(checkRepository.save(any(Check.class))).willReturn(repoReturn);

        // when
        Check returned = service.updateNextExecutionTime(check, result);

        // then
        ArgumentCaptor<Check> captor = ArgumentCaptor.forClass(Check.class);
        then(checkRepository).should().save(captor.capture());

        Check savedArg = captor.getValue();
        assertThat(savedArg)
                .as("в save должен уходить исходный объект проверки")
                .isSameAs(check);
        assertThat(savedArg.getNextRunAt())
                .as("nextRunAt должен быть выставлен как finishedAt + runIntervalMin")
                .isEqualTo(FINISHED_AT.plusMinutes(RUN_INTERVAL_MIN));

        assertThat(returned)
                .as("метод должен вернуть то, что вернул репозиторий")
                .isSameAs(repoReturn);
    }

    @Test
    @DisplayName("должен бросать NPE, если finishedAt=null, и не обращаться к репозиторию")
    void should_throw_npe_when_finishedAt_is_null_and_not_touch_repository() {
        // given
        Check check = new Check();
        check.setRunIntervalMin(5);

        CheckResult result = new CheckResult(); // finishedAt == null

        // when / then
        assertThatThrownBy(() -> service.updateNextExecutionTime(check, result))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(checkRepository);
    }
}