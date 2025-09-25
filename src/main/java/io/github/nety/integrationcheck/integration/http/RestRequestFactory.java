package io.github.nety.integrationcheck.integration.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nety.integrationcheck.dto.RestApiProfileDto;
import io.github.nety.integrationcheck.enums.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Фабрика подготовки {@link RestClient.RequestBodySpec} по профилю REST-интеграции.
 * <p>
 * Конфигурирует:
 * <ul>
 *   <li>HTTP-метод и URL;</li>
 *   <li>Заголовки из JSON;</li>
 *   <li>Таймауты подключения/чтения;</li>
 *   <li>Тело запроса для методов, где это уместно.</li>
 * </ul>
 * <p>
 * Потокобезопасен: использует {@link RestClient.Builder#clone()} и
 * thread-safe {@link ObjectMapper}.
 *
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class RestRequestFactory {

    /**
     * Методы, где уместен body.
     */
    private static final EnumSet<HttpMethod> METHODS_WITH_BODY = EnumSet.of(
            HttpMethod.POST, HttpMethod.PUT
    );

    /**
     * Явный тип для JSON вида: {"Header":["v1","v2"], ...}.
     */
    private static final TypeReference<Map<String, List<String>>> HEADERS_TYPE = new TypeReference<>() {
    };

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Копирует заголовки в целевой {@link HttpHeaders}, пропуская пустые/null значения.
     *
     * @param target  целевые заголовки Spring
     * @param headers источник значений вида Map&lt;name, List&lt;value&gt;&gt;
     */
    private static void copyHeaders(HttpHeaders target, Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) return;
        headers.forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                target.addAll(name, values);
            }
        });
    }

    /**
     * Маппинг доменного {@link HttpMethod} на {@link org.springframework.http.HttpMethod}.
     *
     * @param method доменный метод
     * @return соответствующий метод Spring
     */
    private static org.springframework.http.HttpMethod toSpringMethod(HttpMethod method) {
        return switch (method) {
            case GET -> org.springframework.http.HttpMethod.GET;
            case POST -> org.springframework.http.HttpMethod.POST;
            case PUT -> org.springframework.http.HttpMethod.PUT;
        };
    }

    /**
     * Формирует спецификацию запроса (без выполнения).
     *
     * @param profile профиль вызова: URL, метод, заголовки (JSON), таймауты, тело
     * @return готовый {@link RestClient.RequestBodySpec} для последующего вызова
     * @throws IllegalArgumentException если не удалось разобрать JSON заголовков
     * @apiNote Формат заголовков: {@code {"Header-Name":["v1","v2"]}}
     */
    public RestClient.RequestBodySpec prepare(RestApiProfileDto profile) {
        final RestClient client = buildClient(profile);
        final Map<String, List<String>> headers = parseHeaders(profile.headers());

        RestClient.RequestBodySpec spec = client
                .method(toSpringMethod(profile.httpMethod()))
                .uri(profile.url())
                .headers(h -> copyHeaders(h, headers));

        if (METHODS_WITH_BODY.contains(profile.httpMethod()) && profile.requestBody() != null) {
            spec = spec.body(profile.requestBody());
        }

        return spec;
    }

    /**
     * Создаёт изолированный {@link RestClient} с заданными таймаутами.
     * <ul>
     *   <li>connectTimeout — на уровне {@link HttpClient};</li>
     *   <li>readTimeout — на уровне {@link JdkClientHttpRequestFactory}.</li>
     * </ul>
     *
     * @param profile источник настройки таймаутов
     * @return новый экземпляр {@link RestClient}, построенный из {@link RestClient.Builder#clone()}
     * @implNote writeTimeout можно включить при обновлении версии Spring (см. комментарий в коде).
     */
    private RestClient buildClient(RestApiProfileDto profile) {
        final int timeoutSec = profile.timeoutSeconds();
        final Duration timeout = Duration.ofSeconds(timeoutSec);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return restClientBuilder.clone()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Парсит JSON-представление заголовков.
     *
     * @param raw строка JSON, например {@code {"Accept":["application/json"]}}
     * @return map заголовков; пустая map — если строка пуста/blank
     * @throws IllegalArgumentException если JSON некорректен
     */
    private Map<String, List<String>> parseHeaders(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(raw, HEADERS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Не удалось разобрать JSON заголовков. Исходные данные: %s. Exception: %s"
                            .formatted(raw, exception.getMessage())
            );
        }
    }
}