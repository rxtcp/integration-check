package io.github.rxtcp.integrationcheck.common.net;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link TimeoutDetector#isTimeout(Throwable)}.
 * Структура тестов отражает тип входного исключения и условия сопоставления по сообщению.
 */
@DisplayName("TimeoutDetector.isTimeout(...)")
@DisplayNameGeneration(ReplaceUnderscores.class)
class TimeoutDetectorTest {

    @Nested
    @DisplayName("Обработка граничных значений")
    class EdgeCases {

        @Test
        void should_return_false_when_input_is_null() {
            assertFalse(TimeoutDetector.isTimeout(null));
        }

        @Test
        void should_return_false_when_other_exception_contains_phrase_in_message() {
            assertFalse(TimeoutDetector.isTimeout(new RuntimeException("timed out")));
        }
    }

    @Nested
    @DisplayName("Специализированные таймаут-исключения")
    class SpecificTimeoutExceptions {

        @Test
        void should_return_true_for_SocketTimeoutException() {
            assertTrue(TimeoutDetector.isTimeout(new SocketTimeoutException("read timed out")));
        }

        @Test
        void should_return_true_for_HttpTimeoutException() {
            assertTrue(TimeoutDetector.isTimeout(new HttpTimeoutException("request timed out")));
        }
    }

    @Nested
    @DisplayName("ConnectException: определение по сообщению")
    class ConnectExceptionCases {

        @ParameterizedTest(name = "[{index}] message=''{0}''")
        @ValueSource(strings = {
                "timed out",
                "Timed Out",
                "READ TIMED OUT",
                "operation TiMeD OuT",
                "connect() timed out",
                "Connection attempt timed out after 30s"
        })
        void should_return_true_when_message_contains_phrase_case_insensitive(String message) {
            assertTrue(TimeoutDetector.isTimeout(new ConnectException(message)));
        }

        @Test
        void should_return_true_when_phrase_bridges_words() {
            // 'untimed outage' содержит подстроку 'timed out' через границу слов
            assertTrue(TimeoutDetector.isTimeout(new ConnectException("untimed outage")));
        }

        @ParameterizedTest(name = "[{index}] message=''{0}''")
        @ValueSource(strings = {
                "connection refused",
                "host unreachable",
                "timeout",          // другое слово, не целевая фраза
                "time-out",         // дефис вместо пробела — не совпадение
                "timed-out",        // то же
                "timedout"          // слитно — не совпадает
        })
        void should_return_false_when_message_does_not_contain_target_phrase(String message) {
            assertFalse(TimeoutDetector.isTimeout(new ConnectException(message)));
        }

        @Test
        void should_return_false_when_message_is_null() {
            assertFalse(TimeoutDetector.isTimeout(new ConnectException(null)));
        }
    }

    @Nested
    @DisplayName("Прочие IOException: определение по сообщению")
    class OtherIOExceptionCases {

        @ParameterizedTest(name = "[{index}] message=''{0}''")
        @ValueSource(strings = {
                "timed out",
                "READ TIMED OUT",
                "write timed out",
                "handshake TiMeD OuT"
        })
        void should_return_true_when_message_contains_phrase_case_insensitive(String message) {
            assertTrue(TimeoutDetector.isTimeout(new IOException(message)));
        }

        @Test
        void should_return_true_when_phrase_bridges_words() {
            // 'untimed outage' содержит подстроку 'timed out' через границу слов
            assertTrue(TimeoutDetector.isTimeout(new IOException("untimed outage")));
        }

        @ParameterizedTest(name = "[{index}] message=''{0}''")
        @ValueSource(strings = {
                "",                 // пустое сообщение
                "connection closed",
                "timeout",          // не целевая фраза
                "time-out",         // дефис вместо пробела
                "timed  out"        // двойной пробел — не совпадает с 'timed out'
        })
        void should_return_false_when_message_does_not_contain_target_phrase(String message) {
            assertFalse(TimeoutDetector.isTimeout(new IOException(message)));
        }

        @Test
        void should_return_false_when_message_is_null() {
            assertFalse(TimeoutDetector.isTimeout(new IOException((String) null)));
        }
    }
}