package io.github.rxtcp.integrationcheck.service.processor.strategy;

import io.github.rxtcp.integrationcheck.common.net.TimeoutDetector;
import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.integration.http.RestRequestFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.FAILED;
import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.SUCCEEDED;
import static io.github.rxtcp.integrationcheck.enums.FailureReason.ERROR;
import static io.github.rxtcp.integrationcheck.enums.FailureReason.HTTP_STATUS_MISMATCH;
import static io.github.rxtcp.integrationcheck.enums.FailureReason.TIMEOUT;

/**
 * Стратегия проверки REST API ({@link CheckType#REST_API}).
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RestApiCheckStrategy implements CheckStrategy {

    /**
     * Фабрика запросов для {@link RestClient}.
     */
    private final RestRequestFactory restRequestFactory;

    /**
     * Поддерживаемый тип проверки.
     */
    @Override
    public CheckType getType() {
        return CheckType.REST_API;
    }

    /**
     * Выполнить проверку REST API.
     *
     * @param check профиль и параметры проверки
     * @return результат выполнения
     */
    @Override
    public CheckResultDto execute(CheckDto check) {
        var restApiProfile = (RestApiProfileDto) check.profile();
        try {
            log.info("Выполнение проверки профиля REST API: {}", restApiProfile);
            RestClient.RequestBodySpec spec = restRequestFactory.prepare(restApiProfile);
            ResponseEntity<String> responseEntity = spec.retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> { /* обработка в buildCheckResult */ })
                    .toEntity(String.class);
            log.info("Получен HTTP ответ: код = {}, тело = {}", responseEntity.getStatusCode(), responseEntity.getBody());
            return buildCheckResult(restApiProfile, responseEntity);
        } catch (Exception exception) {
            return buildFailedCheckResult(exception);
        }
    }

    /**
     * Сопоставляет фактический HTTP-статус с ожидаемым и формирует результат.
     */
    private CheckResultDto buildCheckResult(RestApiProfileDto restApiProfile, ResponseEntity<String> responseEntity) {
        var expectedHttpCode = restApiProfile.expectedHttpCode();
        var actualHttpCode = responseEntity.getStatusCode().value();

        if (actualHttpCode == expectedHttpCode) {
            return new CheckResultDto(SUCCEEDED, null, responseEntity.getBody());
        }

        return new CheckResultDto(
                FAILED,
                HTTP_STATUS_MISMATCH,
                "Ожидаемый HTTP код = %d, но получен HTTP код = %d. %s"
                        .formatted(expectedHttpCode, actualHttpCode, responseEntity.getBody())
        );
    }

    /**
     * Маппит исключения на причины сбоя (таймаут/ошибка) и логирует.
     */
    private CheckResultDto buildFailedCheckResult(Exception exception) {
        final Throwable root = NestedExceptionUtils.getMostSpecificCause(exception);

        if (TimeoutDetector.isTimeout(root)) {
            log.error("Таймаут при обращении к REST API: {}", root.toString());
            return new CheckResultDto(FAILED, TIMEOUT, root.getMessage());
        }

        log.error("Возникла ошибка во время выполнения проверки REST API", exception);
        return new CheckResultDto(FAILED, ERROR, exception.getMessage());
    }
}
