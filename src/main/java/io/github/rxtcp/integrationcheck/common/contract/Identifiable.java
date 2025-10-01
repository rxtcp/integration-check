package io.github.rxtcp.integrationcheck.common.contract;

/**
 * Контракт для объектов, имеющих уникальный идентификатор.
 *
 * <p>Идентификатор может быть {@code null} до присвоения (например, до сохранения в БД).</p>
 *
 * @param <ID> тип идентификатора (например, {@link java.util.UUID} или {@link Long})
 */
@FunctionalInterface
public interface Identifiable<ID> {

    /**
     * Возвращает уникальный идентификатор сущности.
     *
     * @return идентификатор, либо {@code null}, если ещё не присвоен
     */
    ID getId();
}