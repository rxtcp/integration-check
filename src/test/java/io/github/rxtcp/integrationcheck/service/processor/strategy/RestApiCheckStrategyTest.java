package io.github.rxtcp.integrationcheck.service.processor.strategy;

import io.github.rxtcp.integrationcheck.common.net.TimeoutDetector;
import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.enums.FailureReason;
import io.github.rxtcp.integrationcheck.enums.HttpMethod;
import io.github.rxtcp.integrationcheck.integration.http.RestRequestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты стратегии REST-проверок.
 * Проверяем:
 * - корректный тип стратегии;
 * - успешный сценарий (коды совпали, тело прокинулось);
 * - обработку несовпадающего кода ответа;
 * - маппинг таймаута в FAILURE=TIMEOUT;
 * - маппинг прочих ошибок в FAILURE=ERROR.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestApiCheckStrategy")
@DisplayNameGeneration(ReplaceUnderscores.class)
class RestApiCheckStrategyTest {

    @Mock
    private RestRequestFactory restRequestFactory;
    @Mock
    private RestClient.RequestBodySpec requestSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private RestApiCheckStrategy strategy;

    // ===== Хелперы =====

    private static RestApiProfileDto newProfile(int expectedCode) {
        return new RestApiProfileDto(
                /*checkId*/ 10L,
                /*profileId*/ 20L,
                "https://example.org/health",
                HttpMethod.GET,
                /*timeoutSec*/ 5,
                /*headers*/ "{\"X-Trace-Id\":[\"abc\"]}",
                /*body*/ null,
                expectedCode
        );
    }

    private static CheckDto newInput(RestApiProfileDto p) {
        // Для стратегии важны поля type/profile; остальное задаём валидными значениями.
        return new CheckDto(
                1L, "rest", "desc", true, 1,
                LocalDateTime.now().plusMinutes(1),
                CheckType.REST_API,
                p
        );
    }

    /**
     * Настраивает «плоскую» fluent-цепочку RestClient и возвращает заданный HTTP-ответ.
     */
    private void stubFluentChainReturning(ResponseEntity<String> entity) {
        when(restRequestFactory.prepare(any(RestApiProfileDto.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        // onStatus возвращает тот же ResponseSpec, чтобы цепочка была единообразной
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(entity);
    }

    // ===== Тесты =====

    @Test
    @DisplayName("getType() → REST_API")
    void getType_returns_REST_API() {
        assertThat(strategy.getType()).isEqualTo(CheckType.REST_API);
    }

    @Test
    @DisplayName("execute(): SUCCEEDED когда ожидаемый HTTP-код совпал с фактическим")
    void execute_returns_succeeded_when_expected_code_matches_actual() {
        // given
        var profile = newProfile(200);
        var input = newInput(profile);
        stubFluentChainReturning(new ResponseEntity<>("OK", HttpStatus.OK));

        // when
        CheckResultDto result = strategy.execute(input);

        // then
        // Проверяем цепочку вызовов в ожидаемом порядке
        InOrder inOrder = inOrder(restRequestFactory, requestSpec, responseSpec);
        inOrder.verify(restRequestFactory).prepare(same(profile));
        inOrder.verify(requestSpec).retrieve();
        inOrder.verify(responseSpec).onStatus(any(), any());
        inOrder.verify(responseSpec).toEntity(String.class);
        inOrder.verifyNoMoreInteractions();

        assertThat(result.status()).isEqualTo(CheckRunStatus.SUCCEEDED);
        assertThat(result.failureReason()).isNull();
        assertThat(result.details()).isEqualTo("OK");
    }

    @Test
    @DisplayName("execute(): FAILED/HTTP_STATUS_MISMATCH при несоответствии HTTP-кода")
    void execute_returns_failed_with_http_status_mismatch_on_code_difference() {
        // given
        var profile = newProfile(200);
        var input = newInput(profile);
        stubFluentChainReturning(new ResponseEntity<>("ERR-BODY", HttpStatus.INTERNAL_SERVER_ERROR));

        // when
        CheckResultDto result = strategy.execute(input);

        // then
        assertThat(result.status()).isEqualTo(CheckRunStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo(FailureReason.HTTP_STATUS_MISMATCH);
        assertThat(result.details())
                .contains("Ожидаемый HTTP код = 200")
                .contains("получен HTTP код = 500")
                .contains("ERR-BODY");
    }

    @Test
    @DisplayName("execute(): FAILED/TIMEOUT при сетевом таймауте")
    void execute_returns_failed_timeout_on_network_timeout() {
        // given
        var profile = newProfile(200);
        var input = newInput(profile);

        RuntimeException timeout = new RuntimeException("socket timeout");
        when(restRequestFactory.prepare(any(RestApiProfileDto.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenThrow(timeout);

        // Мокаем статический детектор таймаута и отмечаем именно наш exception как таймаут.
        try (MockedStatic<TimeoutDetector> mocked = mockStatic(TimeoutDetector.class)) {
            mocked.when(() -> TimeoutDetector.isTimeout(any()))
                    .thenAnswer(inv -> inv.getArgument(0) == timeout);

            // when
            CheckResultDto result = strategy.execute(input);

            // then
            assertThat(result.status()).isEqualTo(CheckRunStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(FailureReason.TIMEOUT);
            assertThat(result.details()).containsIgnoringCase("timeout");
        }
    }

    @Test
    @DisplayName("execute(): FAILED/ERROR для прочих исключений")
    void execute_returns_failed_error_on_other_exceptions() {
        // given
        var profile = newProfile(201);
        var input = newInput(profile);

        IllegalStateException arbitrary = new IllegalStateException("boom");
        when(restRequestFactory.prepare(any(RestApiProfileDto.class))).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenThrow(arbitrary);

        try (MockedStatic<TimeoutDetector> mocked = mockStatic(TimeoutDetector.class)) {
            mocked.when(() -> TimeoutDetector.isTimeout(any())).thenReturn(false);

            // when
            CheckResultDto result = strategy.execute(input);

            // then
            assertThat(result.status()).isEqualTo(CheckRunStatus.FAILED);
            assertThat(result.failureReason()).isEqualTo(FailureReason.ERROR);
            assertThat(result.details()).contains("boom");
        }
    }
}