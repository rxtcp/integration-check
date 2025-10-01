package io.github.rxtcp.integrationcheck.integration.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link RestRequestFactory}.
 * <p>
 * Фокус:
 * - метод {@code prepare(...)} корректно настраивает HTTP-метод, URL, заголовки и тело;
 * - JSON заголовков валидируется и преобразуется; пустые/некорректные значения игнорируются;
 * - клиент собирается с ожидаемыми таймаутами чтения/соединения.
 */
@DisplayName("RestRequestFactory")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class RestRequestFactoryTest {

    // ===== Тестовые константы (говорящие имена) =====
    private static final String URL = "https://api.example.com/items";
    private static final int TIMEOUT_SEC = 15;
    private static final String RAW_HEADERS_JSON =
            "{\"Accept\":[\"application/json\"],\"X-Trace-Id\":[\"a\",\"b\"]}";

    // ===== Моки зависимостей Spring =====
    private final RestClient.Builder rootBuilder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
    private final RestClient.Builder clonedBuilder = mock(RestClient.Builder.class, RETURNS_DEEP_STUBS);
    private final RestClient restClient = mock(RestClient.class);
    private final RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class, RETURNS_SELF);
    private final RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class, RETURNS_SELF);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    /**
     * Хелпер: создаёт реальный DTO-профиль (record) для теста.
     */
    private static RestApiProfileDto profile(String url,
                                             HttpMethod method,
                                             String headersJson,
                                             int timeoutSec,
                                             String requestBody) {
        // checkId/profileId/expectedHttpCode конкретные значения не важны в этих тестах
        return new RestApiProfileDto(1L, 2L, url, method, timeoutSec, headersJson, requestBody, 200);
    }

    private static void assertReadTimeout(JdkClientHttpRequestFactory rf, int timeoutSec) {
        try {
            Duration readTimeout = (Duration) JdkClientHttpRequestFactory.class
                    .getMethod("getReadTimeout")
                    .invoke(rf);
            assertThat(readTimeout).isEqualTo(Duration.ofSeconds(timeoutSec));
        } catch (ReflectiveOperationException noGetter) {
            try {
                Field f = JdkClientHttpRequestFactory.class.getDeclaredField("readTimeout");
                f.setAccessible(true);
                Duration readTimeout = (Duration) f.get(rf);
                assertThat(readTimeout).isEqualTo(Duration.ofSeconds(timeoutSec));
            } catch (Exception e) {
                fail("Не удалось проверить readTimeout через рефлексию", e);
            }
        }
    }

    private static void assertConnectTimeout(JdkClientHttpRequestFactory rf, int timeoutSec) {
        try {
            Field f = JdkClientHttpRequestFactory.class.getDeclaredField("httpClient");
            f.setAccessible(true);
            HttpClient httpClient = (HttpClient) f.get(rf);
            assertThat(httpClient.connectTimeout())
                    .isPresent()
                    .contains(Duration.ofSeconds(timeoutSec));
        } catch (Exception e) {
            fail("Не удалось проверить connectTimeout у HttpClient через рефлексию", e);
        }
    }

    // Фабрика тестируемого объекта: единообразно настраивает цепочку билдера/клиента
    private RestRequestFactory newFactory() {
        when(rootBuilder.clone()).thenReturn(clonedBuilder);
        when(clonedBuilder.requestFactory(any())).thenReturn(clonedBuilder);
        when(clonedBuilder.build()).thenReturn(restClient);

        when(restClient.method(any())).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(bodySpec);

        return new RestRequestFactory(rootBuilder, objectMapper);
    }

    /**
     * Хелпер: применяет захваченный consumer заголовков к экземпляру HttpHeaders и возвращает его.
     */
    @SuppressWarnings("unchecked")
    private HttpHeaders applyCapturedHeaders() {
        ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
        when(bodySpec.headers(headersCaptor.capture())).thenReturn(bodySpec);
        return headersCaptor.getAllValues().isEmpty() ? new HttpHeaders() : new HttpHeaders();
    }

    // ===== Утилиты для стабильных проверок таймаутов =====

    // ===== Сценарии prepare(...) =====
    @Nested
    @DisplayName("prepare(...)")
    class PrepareSpecs {

        @Test
        void should_configure_method_url_and_headers_and_return_spec() throws Exception {
            RestRequestFactory factory = newFactory();

            Map<String, List<String>> parsedHeaders = Map.of(
                    "Accept", List.of("application/json"),
                    "X-Trace-Id", List.of("a", "b")
            );
            //noinspection unchecked
            when(objectMapper.readValue(eq(RAW_HEADERS_JSON), any(TypeReference.class)))
                    .thenReturn(parsedHeaders);

            // Захватываем consumer заголовков
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(bodySpec.headers(headersCaptor.capture())).thenReturn(bodySpec);

            RestApiProfileDto dto = profile(URL, HttpMethod.GET, RAW_HEADERS_JSON, 7, null);

            RestClient.RequestBodySpec result = factory.prepare(dto);

            // Проверяем построение клиента и цепочку вызовов
            verify(rootBuilder).clone();
            verify(clonedBuilder).requestFactory(any(JdkClientHttpRequestFactory.class));
            verify(clonedBuilder).build();
            verify(restClient).method(org.springframework.http.HttpMethod.GET);
            verify(uriSpec).uri(URL);
            verify(bodySpec).headers(any());

            // Применяем захваченный consumer и убеждаемся, что заголовки проставлены
            HttpHeaders springHeaders = new HttpHeaders();
            headersCaptor.getValue().accept(springHeaders);
            assertThat(springHeaders.getAccept()).containsExactly(MediaType.APPLICATION_JSON);
            assertThat(springHeaders.get("X-Trace-Id")).containsExactly("a", "b");

            // Возвращён ровно тот же spec, что и в цепочке
            assertThat(result).isSameAs(bodySpec);

            verify(objectMapper, times(1)).readValue(eq(RAW_HEADERS_JSON), any(TypeReference.class));
        }

        @Test
        void should_skip_blank_headers_and_not_call_objectmapper() throws JsonProcessingException {
            RestRequestFactory factory = newFactory();
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(bodySpec.headers(headersCaptor.capture())).thenReturn(bodySpec);

            RestApiProfileDto dto = profile("https://host", HttpMethod.GET, "   ", 5, null);

            factory.prepare(dto);

            HttpHeaders springHeaders = new HttpHeaders();
            headersCaptor.getValue().accept(springHeaders);
            assertThat(springHeaders).isEmpty();

            verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
        }

        @Test
        void should_copy_only_non_empty_header_values() throws Exception {
            RestRequestFactory factory = newFactory();

            Map<String, List<String>> parsed = new LinkedHashMap<>();
            parsed.put("Empty", Collections.emptyList());
            parsed.put("Null", null);
            parsed.put("Valid", List.of("v1", "v2"));
            //noinspection unchecked
            when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(parsed);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
            when(bodySpec.headers(headersCaptor.capture())).thenReturn(bodySpec);

            RestApiProfileDto dto = profile("https://host", HttpMethod.GET, "{\"stub\":true}", 3, null);
            factory.prepare(dto);

            HttpHeaders h = new HttpHeaders();
            headersCaptor.getValue().accept(h);

            assertThat(h).hasSize(1);
            assertThat(h.get("Valid")).containsExactly("v1", "v2");
            assertThat(h.get("Empty")).isNull();
            assertThat(h.get("Null")).isNull();
        }

        @ParameterizedTest(name = "should_set_body_only_for_{0}")
        @EnumSource(value = HttpMethod.class, names = {"POST", "PUT"})
        void should_set_body_only_for_post_put(HttpMethod method) throws Exception {
            RestRequestFactory factory = newFactory();
            //noinspection unchecked
            when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of());

            RestApiProfileDto dto = profile("https://host", method, "{}", 10, "payload");

            factory.prepare(dto);

            verify(bodySpec, times(1)).body(eq("payload"));
        }

        @Test
        void should_not_call_body_for_get_and_for_post_put_without_payload() throws Exception {
            RestRequestFactory factory = newFactory();
            //noinspection unchecked
            when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(Map.of());

            // GET + тело → body не вызывается
            RestApiProfileDto getWithBody = profile("https://host", HttpMethod.GET, "{}", 10, "payload");
            factory.prepare(getWithBody);
            // POST без тела → body не вызывается
            RestApiProfileDto postNoBody = profile("https://host", HttpMethod.POST, "{}", 10, null);
            factory.prepare(postNoBody);

            verify(bodySpec, never()).body(any());
        }

        @Test
        void should_throw_iae_when_headers_json_is_invalid_and_message_contains_raw_input() throws Exception {
            RestRequestFactory factory = newFactory();

            String raw = "{oops}";
            JsonProcessingException jpe = new JsonProcessingException("boom") {
            };
            //noinspection unchecked
            when(objectMapper.readValue(eq(raw), any(TypeReference.class))).thenThrow(jpe);

            RestApiProfileDto dto = profile("https://host", HttpMethod.GET, raw, 2, null);

            assertThatThrownBy(() -> factory.prepare(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Не удалось разобрать JSON заголовков")
                    .hasMessageContaining(raw)
                    .hasMessageContaining("boom");
        }
    }

    // ===== Сценарии buildClient(...) опосредованно через prepare(...) =====
    @Nested
    @DisplayName("buildClient(...) via prepare(...)")
    class BuildClientSpecs {

        @Test
        void should_configure_jdk_request_factory_with_expected_timeouts() {
            RestRequestFactory factory = newFactory();

            RestApiProfileDto dto = profile("https://host", HttpMethod.GET, null, TIMEOUT_SEC, null);

            factory.prepare(dto);

            // Захватываем переданный requestFactory
            ArgumentCaptor<JdkClientHttpRequestFactory> rfCaptor = ArgumentCaptor.forClass(JdkClientHttpRequestFactory.class);
            verify(clonedBuilder).requestFactory(rfCaptor.capture());

            JdkClientHttpRequestFactory rf = rfCaptor.getValue();
            assertThat(rf).isNotNull();

            assertReadTimeout(rf, TIMEOUT_SEC);
            assertConnectTimeout(rf, TIMEOUT_SEC);
        }
    }
}