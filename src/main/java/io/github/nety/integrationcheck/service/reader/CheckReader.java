package io.github.nety.integrationcheck.service.reader;

import io.github.nety.integrationcheck.entity.Check;

import java.util.List;

/**
 * Чтение проверок из источника данных.
 */
public interface CheckReader {

    /**
     * Идентификаторы проверок, срок запуска которых наступил.
     */
    List<Long> findDueIds();

    /**
     * Получить проверку по идентификатору.
     * Поведение при отсутствии записи — на усмотрение реализации.
     */
    Check findById(long id);
}
