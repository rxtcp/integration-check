package io.github.rxtcp.integrationcheck.service.reader;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Сервис чтения проверок.
 */
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = READ_COMMITTED, propagation = REQUIRES_NEW)
@Service
public class CheckReaderService implements CheckReader {

    /**
     * Репозиторий {@link Check}.
     */
    private final CheckRepository checkRepository;

    /**
     * Идентификаторы проверок, готовых к запуску.
     */
    @Override
    public List<Long> findDueIds() {
        return checkRepository.findDueCheckIds();
    }

    /**
     * Найти проверку вместе с необходимым профилем по идентификатору.
     *
     * @param id идентификатор
     * @return сущность {@link Check}
     * @throws EntityNotFoundException если запись не найдена
     */
    @Override
    public Check findWithProfileById(long id) {
        return checkRepository.findWithProfileById(id)
                .orElseThrow(() -> new EntityNotFoundException("Не найдена сущность Check: id=%d".formatted(id)));
    }
}
