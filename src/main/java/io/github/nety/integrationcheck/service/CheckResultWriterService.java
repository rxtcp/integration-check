package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.CheckResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CheckResultWriterService implements CheckResultWriter {

    @Override
    public CheckResult saveResult(CheckResult results) {
        return List.of();
    }
}
