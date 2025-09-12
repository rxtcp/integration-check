package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.CheckResult;
import io.github.nety.integrationcheck.repository.CheckResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CheckResultWriterService implements CheckResultWriter {

    private final CheckResultRepository checkResultRepository;

    @Override
    public CheckResult saveResult(CheckResult checkResult) {
        return checkResultRepository.save(checkResult);
    }
}
