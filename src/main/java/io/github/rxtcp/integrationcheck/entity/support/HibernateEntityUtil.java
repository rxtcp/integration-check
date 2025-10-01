package io.github.rxtcp.integrationcheck.entity.support;

import io.github.rxtcp.integrationcheck.common.contract.Identifiable;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/**
 * Утилиты для работы с Hibernate-сущностями.
 *
 * <p>Особенности:
 * <ul>
 *   <li>Null-safe методы (не инициализируют ленивые прокси).</li>
 *   <li>Поддержка как реальных сущностей, так и {@link HibernateProxy}.</li>
 * </ul>
 */
public final class HibernateEntityUtil {

    private HibernateEntityUtil() {
    }

    /**
     * Возвращает идентификатор сущности типа {@link Long}.
     *
     * <p>Поведение:
     * <ul>
     *   <li>{@code null} → {@code null};</li>
     *   <li>{@link HibernateProxy} → ID из {@code HibernateLazyInitializer} (без инициализации прокси);</li>
     *   <li>{@link Identifiable} → {@code getId()};</li>
     *   <li>Иначе → {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * @param entity сущность или прокси
     * @return идентификатор или {@code null}, если {@code entity == null}
     * @throws IllegalArgumentException если тип сущности не поддерживается
     * @implNote Ожидается, что идентификатор сущности имеет тип {@link Long}.
     */
    public static Long idOf(Object entity) {
        return switch (entity) {
            case null -> null;
            case HibernateProxy proxy -> (Long) proxy.getHibernateLazyInitializer().getIdentifier();
            case Identifiable<?> identifiable -> (Long) identifiable.getId();
            default -> throw new IllegalArgumentException("Unsupported entity type: " + entity.getClass());
        };
    }

    /**
     * Возвращает простое имя реального класса сущности (с разворачиванием прокси Hibernate).
     *
     * <p>Для {@code null} возвращает строку {@code "null"}.
     *
     * @param entity сущность или прокси
     * @return простое имя класса без пакета или {@code "null"}
     */
    public static String simpleClassName(Object entity) {
        return entity == null ? Objects.toString(null) : Hibernate.getClass(entity).getSimpleName();
    }
}
