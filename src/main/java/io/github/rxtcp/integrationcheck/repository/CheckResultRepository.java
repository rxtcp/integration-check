package io.github.rxtcp.integrationcheck.repository;

import io.github.rxtcp.integrationcheck.entity.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для доступа к {@link CheckResult}. Тип идентификатора — {@link Long}.
 */
@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {
}