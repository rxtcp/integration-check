package io.github.rxtcp.integrationcheck.service.reader;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("CheckReaderService — интеграция")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckReaderServiceIntegrationTest {

    private static final int RUN_INTERVAL_MIN = 5;

    private final CheckReaderService service;
    private final CheckRepository checkRepository;

    CheckReaderServiceIntegrationTest(CheckReaderService service, CheckRepository checkRepository) {
        this.service = service;
        this.checkRepository = checkRepository;
    }

    // ========================= ТЕСТЫ =========================

    /**
     * Создаёт профиль REST API со всеми обязательными полями.
     */
    private static RestApiProfile newRestProfile() {
        RestApiProfile p = new RestApiProfile();
        p.setUrl("https://example.org/health");
        p.setHttpMethod(HttpMethod.values()[0]);
        p.setTimeoutSeconds(10);
        p.setHeaders("{\"X-Trace-Id\":[\"abc\"]}");
        p.setRequestBody(null);
        p.setExpectedHttpCode(200);
        return p;
    }

    @Test
    @DisplayName("findDueIds: возвращает только включённые и «просроченные» проверки")
    void should_return_only_enabled_and_due_ids() {
        LocalDateTime now = LocalDateTime.now();

        Check due = persistCheck(true, now.minusMinutes(1));
        Check future = persistCheck(true, now.plusMinutes(30));
        Check disabled = persistCheck(false, now.minusMinutes(1));

        List<Long> ids = service.findDueIds();

        assertThat(ids).contains(due.getId());
        assertThat(ids).doesNotContain(future.getId(), disabled.getId());
    }

    @Test
    @DisplayName("findWithProfileById: возвращает сущность с подгруженным профилем")
    void should_return_entity_with_profile_loaded() {
        Check check = persistCheck(true, LocalDateTime.now().minusMinutes(5));

        // привязываем профиль и сохраняем каскадно
        RestApiProfile profile = newRestProfile();
        check.attachProfile(profile, CheckType.values()[0]);
        checkRepository.saveAndFlush(check);

        Check loaded = service.findWithProfileById(check.getId());

        assertThat(loaded).isNotNull();
        assertThat(loaded.getId()).isEqualTo(check.getId());
        assertThat(loaded.getProfile()).isNotNull();
        assertThat(loaded.getProfile().getId()).isNotNull();
        // Обратную ссылку (loaded.getProfile().getCheck()) не трогаем, чтобы не ловить LazyInitialization вне TX.
    }

    // ======================= ФИКСТУРЫ ========================

    @Test
    @DisplayName("findWithProfileById: при отсутствии бросает EntityNotFoundException с id")
    void should_throw_EntityNotFoundException_when_entity_absent() {
        long unknownId = 9_999_999L;

        assertThatThrownBy(() -> service.findWithProfileById(unknownId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("id=%d".formatted(unknownId));
    }

    /**
     * Сохраняет Check с уникальным именем, чтобы избежать конфликтов на CI.
     */
    private Check persistCheck(boolean enabled, LocalDateTime nextRunAt) {
        Check c = Check.builder()
                .name("svc-" + UUID.randomUUID())
                .description("desc")
                .enabled(enabled)
                .runIntervalMin(RUN_INTERVAL_MIN)
                .nextRunAt(nextRunAt)
                .type(CheckType.values()[0]) // не завязываемся на конкретное имя enum-константы
                .build();
        return checkRepository.saveAndFlush(c);
    }
}