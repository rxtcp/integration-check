package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.CheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {
}
