package io.github.rxtcp.integrationcheck.service.reader;

import io.github.rxtcp.integrationcheck.entity.Check;

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
     * Найти проверку вместе с необходимым профилем по идентификатору.
     * Поведение при отсутствии записи — на усмотрение реализации.
     */
    Check findWithProfileById(long id);
}
