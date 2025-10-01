package io.github.rxtcp.integrationcheck.common.net;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Locale;

/**
 * Определяет, связано ли исключение с истечением сетевого таймаута.
 *
 * <p>Таймаутом считаются:
 * <ul>
 *   <li>{@link SocketTimeoutException}</li>
 *   <li>{@link HttpTimeoutException}</li>
 *   <li>{@link ConnectException} и прочие {@link IOException}, если сообщение содержит фразу {@code "timed out"} (без учёта регистра)</li>
 * </ul>
 * Класс статический и потокобезопасный.</p>
 *
 * <p>Полезно для унификации ретраев, метрик и логирования.</p>
 */
public final class TimeoutDetector {

    /**
     * Фраза-индикатор таймаута в сообщениях исключений.
     */
    private static final String TIMEOUT_PHRASE = "timed out";

    private TimeoutDetector() {
    }

    /**
     * Возвращает {@code true}, если исключение указывает на истечение таймаута.
     * Для {@code null} возвращает {@code false}.
     *
     * @param throwable анализируемое исключение (может быть {@code null})
     * @return {@code true}, если исключение связано с таймаутом; иначе {@code false}
     */
    public static boolean isTimeout(Throwable throwable) {
        return switch (throwable) {
            case SocketTimeoutException e -> true;
            case HttpTimeoutException e -> true;
            case ConnectException e when containsTimeoutPhrase(throwable.getMessage()) -> true;
            case IOException e when containsTimeoutPhrase(throwable.getMessage()) -> true;
            case null, default -> false;
        };
    }

    /**
     * Проверяет наличие фразы-индикатора в сообщении исключения
     * (регистронезависимо, с {@link Locale#ROOT}).
     */
    private static boolean containsTimeoutPhrase(String message) {
        if (message == null) {
            return false;
        }
        final String m = message.toLowerCase(Locale.ROOT);
        return m.contains(TIMEOUT_PHRASE);
    }
}
