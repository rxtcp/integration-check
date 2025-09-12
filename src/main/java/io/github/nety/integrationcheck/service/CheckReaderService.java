package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.repository.CheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CheckReaderService implements CheckReader{

    private final CheckRepository checkRepository;

    @Override
    public List<Check> readChecks() {
        return checkRepository.findDueChecks();
    }
}
