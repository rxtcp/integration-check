package io.github.rxtcp.integrationcheck.service.processor.strategy;

import io.github.rxtcp.integrationcheck.domain.CheckType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Фабрика стратегий проверок: маппит {@link CheckType} → {@link CheckStrategy}.
 */
@Component
public class CheckStrategyFactory {

    /**
     * Реестр стратегий по типу.
     */
    private final Map<CheckType, CheckStrategy> strategies;

    /**
     * Регистрирует все бины {@link CheckStrategy}.
     * Дубликаты типов приводят к {@link IllegalStateException}.
     */
    public CheckStrategyFactory(List<CheckStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        CheckStrategy::getType,
                        Function.identity()
                ));
    }

    /**
     * Возвращает стратегию по типу.
     * @param type тип проверки
     * @return стратегия или {@code null}, если не найдена
     */
    public CheckStrategy getStrategy(CheckType type) {
        return strategies.get(type);
    }
}
