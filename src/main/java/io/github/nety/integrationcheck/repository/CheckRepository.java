package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.Check;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckRepository extends JpaRepository<Check, Long> {

    @Query("""
            select c.id
            from Check c
            where c.isEnabled = true
              and c.nextRunAt <= CURRENT_TIMESTAMP
            order by c.nextRunAt asc
            """)
    List<Long> findDueCheckIds();

    Check findCheckById(Long id);

    @Query("select TYPE(c) from Check c where c.id = :id")
    Class<? extends Check> findType(@Param("id") Long id);
}
