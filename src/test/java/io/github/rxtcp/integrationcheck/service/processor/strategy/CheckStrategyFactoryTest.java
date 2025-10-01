package io.github.rxtcp.integrationcheck.service.processor.strategy;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты фабрики стратегий проверок.
 * Проверяем:
 * - формирование реестра и выдачу стратегии по типу;
 * - поведение при запросе незарегистрированного/отсутствующего типа;
 * - защиту от регистрации дубликатов типов.
 */
@DisplayName("CheckStrategyFactory")
@DisplayNameGeneration(ReplaceUnderscores.class)
final class CheckStrategyFactoryTest {

    // ----------------- стабы стратегий -----------------

    /**
     * Минимально валидный DTO; профиль не требуется для фабрики.
     */
    private static CheckDto minimalCheckDto(CheckType type) {
        return new CheckDto(
                1L, "n", null, true, 5,
                LocalDateTime.now().plusMinutes(1),
                type,
                null
        );
    }

    // ----------------- хелперы -----------------

    /**
     * Возвращает любой тип, отличный от {@code registered}, либо {@code null}, если в enum единственный элемент.
     */
    private static CheckType unregisteredTypeOf(CheckType registered) {
        for (CheckType t : CheckType.values()) {
            if (t != registered) return t;
        }
        return null;
    }

    @Test
    @DisplayName("создаёт реестр и возвращает стратегию по её типу")
    void creates_registry_and_returns_strategy_by_type() {
        // given
        CheckType type = CheckType.values()[0];
        StubStrategy strategy = new StubStrategy(type, "s1");
        CheckStrategyFactory factory = new CheckStrategyFactory(List.of(strategy));

        // when
        CheckStrategy obtained = factory.getStrategy(type);

        // then
        assertThat(obtained).isSameAs(strategy);
        // «дымовой» прогон — стратегия действительно исполняется
        assertThat(obtained.execute(minimalCheckDto(type)).status())
                .isEqualTo(CheckRunStatus.SUCCEEDED);
    }

    // ----------------- тесты -----------------

    @Test
    @DisplayName("возвращает null для незарегистрированного (или null) типа")
    void returns_null_when_type_is_not_registered_or_null() {
        // given: регистрируем стратегию только для одного типа
        CheckType registered = CheckType.values()[0];
        CheckStrategyFactory factory = new CheckStrategyFactory(List.of(new StubStrategy(registered, "s1")));

        // when/then: запрашиваем другой тип (если он есть), иначе null — в обоих случаях ожидаем null
        CheckType absent = unregisteredTypeOf(registered);
        assertThat(factory.getStrategy(absent)).isNull();
    }

    @Test
    @DisplayName("ошибка при попытке зарегистрировать дублирующиеся типы")
    void throws_on_duplicate_types() {
        // given
        CheckType t = CheckType.values()[0];
        var s1 = new StubStrategy(t, "s1");
        var s2 = new StubStrategy(t, "s2");

        // when/then
        assertThatThrownBy(() -> new CheckStrategyFactory(List.of(s1, s2)))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Простейшая стратегия с фиксированным типом и детерминированным результатом.
     */
    private static final class StubStrategy implements CheckStrategy {
        private final CheckType type;
        private final String name;

        private StubStrategy(CheckType type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public CheckType getType() {
            return type;
        }

        @Override
        public CheckResultDto execute(CheckDto check) {
            // Тело результата не важно для тестов фабрики — возвращаем предсказуемое значение
            return new CheckResultDto(CheckRunStatus.SUCCEEDED, null, "ok:" + name);
        }

        @Override
        public String toString() {
            return "StubStrategy{" + name + "}";
        }
    }
}