package io.github.rxtcp.integrationcheck.service.processor;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckProfileDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.enums.HttpMethod;
import io.github.rxtcp.integrationcheck.service.processor.strategy.CheckStrategy;
import io.github.rxtcp.integrationcheck.service.processor.strategy.CheckStrategyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckProcessorService")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckProcessorServiceUnitTest {

    private static final long CHECK_ID = 42L;
    private static final long PROFILE_ID = 2L;
    private static final String URL = "https://example.org";
    private static final int TIMEOUT_SEC = 10;
    private static final int RUN_INTERVAL_MIN = 5;

    @Mock
    private CheckStrategyFactory strategyFactory;
    @Mock
    private CheckStrategy strategy;

    @InjectMocks
    private CheckProcessorService service;

    // ====== ТЕСТЫ ======

    /**
     * Конструирует валидный DTO проверки заданного типа.
     */
    private static CheckDto newCheckDto(CheckType type) {
        CheckProfileDto profile = new RestApiProfileDto(
                CHECK_ID, PROFILE_ID, URL, HttpMethod.GET, TIMEOUT_SEC, null, null, 200
        );
        return new CheckDto(
                CHECK_ID,
                "name",
                "desc",
                true,
                RUN_INTERVAL_MIN,
                LocalDateTime.now().plusMinutes(RUN_INTERVAL_MIN),
                type,
                profile
        );
    }

    @Test
    @DisplayName("process(): делегирует выбранной стратегии и возвращает её результат")
    void should_delegate_to_strategy_and_return_result() {
        // given
        CheckDto input = newCheckDto(CheckType.REST_API);
        CheckResultDto expected = new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok");

        given(strategyFactory.getStrategy(input.type())).willReturn(strategy);
        given(strategy.execute(input)).willReturn(expected);

        // when
        CheckResultDto actual = service.process(input);

        // then
        then(strategyFactory).should(times(1)).getStrategy(input.type());
        then(strategy).should(times(1)).execute(input);
        then(strategyFactory).shouldHaveNoMoreInteractions();
        then(strategy).shouldHaveNoMoreInteractions();

        assertThat(actual).isSameAs(expected);
    }

    // ====== ВСПОМОГАТЕЛЬНОЕ ======

    @Test
    @DisplayName("process(): пробрасывает исключение, полученное от стратегии")
    void should_propagate_exception_from_strategy() {
        // given
        CheckDto input = newCheckDto(CheckType.REST_API);

        given(strategyFactory.getStrategy(any())).willReturn(strategy);
        given(strategy.execute(any())).willThrow(new IllegalStateException("boom"));

        // when / then
        assertThatThrownBy(() -> service.process(input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");

        then(strategyFactory).should().getStrategy(input.type());
        then(strategy).should().execute(input);
    }
}