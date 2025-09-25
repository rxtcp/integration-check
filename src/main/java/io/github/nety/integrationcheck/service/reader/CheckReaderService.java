package io.github.nety.integrationcheck.service.reader;

import io.github.nety.integrationcheck.entity.Check;
import io.github.nety.integrationcheck.repository.CheckRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис чтения проверок (только чтение).
 */
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
     * Найти проверку по идентификатору.
     *
     * @param id идентификатор
     * @return сущность {@link Check}
     * @throws EntityNotFoundException если запись не найдена
     */
    @Override
    public Check findById(long id) {
        return checkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Не найдена сущность Check: id=%d".formatted(id)));
    }
}
