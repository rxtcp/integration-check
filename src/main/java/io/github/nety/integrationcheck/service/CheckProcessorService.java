package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CheckProcessorService implements CheckProcessor {

    private final CheckStrategyFactory strategyFactory;

    @Override
    public CheckResult process(Check check) {
        return strategyFactory.getStrategy(check.getTypeCode())
                .execute(check);
    }
}
