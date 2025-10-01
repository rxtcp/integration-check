package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для простого сервиса, который строит {@link CheckResult} и делегирует сохранение.
 * <p>
 * Цели:
 * 1) Построенный {@link CheckResult} содержит корректные поля для сценариев успеха/ошибки.
 * 2) Делегирование в {@code Saver.save(...)} выполняется ровно один раз.
 * 3) Времена {@code startedAt} и {@code finishedAt} выставлены и согласованы (start <= finish).
 */
@DisplayName("CheckResultService")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class CheckResultServiceMockTest {

    @Mock
    private Saver saver;

    @Test
    void should_build_result_and_delegate_to_saver_for_success() {
        should_build_result_and_delegate_to_saver(true, CheckRunStatus.SUCCEEDED, "ok");
    }

    @Test
    void should_build_result_and_delegate_to_saver_for_failure() {
        should_build_result_and_delegate_to_saver(false, CheckRunStatus.FAILED, "error");
    }

    // ===== Вспомогательный сценарий: сводим успех/ошибку в общий путь =====
    private void should_build_result_and_delegate_to_saver(boolean success,
                                                           CheckRunStatus expectedStatus,
                                                           String expectedDetails) {
        // given
        final CheckResultService service = new CheckResultService(saver);
        final Check check = ResultEntityFixtures.newCheck("svc");

        // возвращаем тот же объект, что передан в save(...)
        when(saver.save(any())).thenAnswer(inv -> inv.getArgument(0, CheckResult.class));

        // when
        final CheckResult saved = service.startAndFinish(check, success);

        // then
        final ArgumentCaptor<CheckResult> captor = ArgumentCaptor.forClass(CheckResult.class);
        verify(saver, times(1)).save(captor.capture());
        verifyNoMoreInteractions(saver);

        final CheckResult built = captor.getValue();

        // возвращённый из сервиса объект — тот же, что ушёл в saver
        assertThat(saved).isSameAs(built);

        // ссылка на Check сохранена как есть
        assertThat(built.getCheck()).isSameAs(check);

        // статус/детали соответствуют ветке успех/ошибка
        assertThat(built.getStatus()).isEqualTo(expectedStatus);
        assertThat(built.getDetails()).isEqualTo(expectedDetails);

        // времена выставлены и согласованы (startedAt <= finishedAt)
        assertThat(built.getStartedAt()).isNotNull();
        assertThat(built.getFinishedAt()).isNotNull();
        assertThat(built.getStartedAt()).isBeforeOrEqualTo(built.getFinishedAt());
    }

    /**
     * Зависимость сервиса: абстракция над хранилищем.
     */
    interface Saver {
        CheckResult save(CheckResult r);
    }

    /**
     * Тестируемый сервис.
     */
    static class CheckResultService {
        private final Saver saver;

        CheckResultService(Saver saver) {
            this.saver = saver;
        }

        public CheckResult startAndFinish(Check check, boolean success) {
            CheckResult r = CheckResult.builder()
                    .check(check)
                    .startedAt(LocalDateTime.now())
                    .finishedAt(LocalDateTime.now())
                    .status(success ? CheckRunStatus.SUCCEEDED : CheckRunStatus.FAILED)
                    .details(success ? "ok" : "error")
                    .build();
            return saver.save(r);
        }
    }
}