package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.CheckLock;
import io.github.nety.integrationcheck.domain.CheckLockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckLockRepository extends JpaRepository<CheckLock, CheckLockId> {
    Optional<CheckLock> findByIdCheckIdAndIdClusterId(Long checkId, String clusterId);
}