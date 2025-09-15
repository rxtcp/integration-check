package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CheckReader {

    List<Long> findDueCheckIds();

    Check findCheckById(Long id);
}
