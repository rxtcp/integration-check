package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.RestApiCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestApiCheckRepository extends JpaRepository<RestApiCheck, Long> {

    @Override
    Optional<RestApiCheck> findById(Long aLong);
}
