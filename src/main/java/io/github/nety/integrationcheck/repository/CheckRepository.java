package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.entity.Check;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий проверок.
 */
@Repository
public interface CheckRepository extends JpaRepository<Check, Long> {

    /** Идентификаторы активных проверок, срок запуска которых наступил (enabled=true и nextRunAt ≤ now). */
    @Query("""
            select c.id
            from Check c
            where c.enabled = true
              and c.nextRunAt <= CURRENT_TIMESTAMP
            """)
    List<Long> findDueCheckIds();

    /**
     * Найти проверку по id с подгруженным профилем (EntityGraph: profile).
     */
    @EntityGraph(attributePaths = "profile")
    @NonNull
    @Override
    Optional<Check> findById(@NonNull Long id);
}
