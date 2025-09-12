package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.Check;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckRepository extends JpaRepository<Check, Long> {

    @Query("""
            select c
            from Check c
            where c.isEnabled = true
              and c.nextRunAt <= CURRENT_TIMESTAMP
            order by c.nextRunAt asc
            """)
    List<Check> findDueChecks();
}
