package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@RequiredArgsConstructor
@Service
public class CheckProcessorService implements CheckProcessor {

    private final CheckStrategyFactory strategyFactory;

    @Transactional(propagation = NOT_SUPPORTED)
    @Override
    public CheckResult process(Check check) {
        return strategyFactory.getStrategy(check.getTypeCode())
                .execute(check);
    }
}
