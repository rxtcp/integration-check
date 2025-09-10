package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.domain.CheckType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CheckStrategyFactory {

    private final Map<CheckType, CheckStrategy> strategies;

    public CheckStrategyFactory(List<CheckStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        CheckStrategy::getType,
                        Function.identity()
                ));
    }

    public CheckStrategy getStrategy(CheckType type) {
        return strategies.get(type);
    }
}