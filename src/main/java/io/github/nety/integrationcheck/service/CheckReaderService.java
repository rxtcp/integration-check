package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.RestApiCheckDto;
import io.github.nety.integrationcheck.repository.CheckRepository;
import io.github.nety.integrationcheck.repository.RestApiCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CheckReaderService implements CheckReader{

    private final CheckRepository checkRepository;
    private final RestApiCheckRepository restApiCheckRepository;

    @Override
    public List<Long> findDueCheckIds() {
        return checkRepository.findDueCheckIds();
    }

    @Override
    public Check findCheckById(Long id) {
        return checkRepository.findCheckById(id);
    }
}
