package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import io.github.nety.integrationcheck.domain.CheckType;
import org.springframework.stereotype.Service;

@Service
public class HttpCheckStrategy implements CheckStrategy {

    @Override
    public CheckType getType() {
        return CheckType.HTTP;
    }

    @Override
    public CheckResult execute(Check check) {
        return null;
    }
}
