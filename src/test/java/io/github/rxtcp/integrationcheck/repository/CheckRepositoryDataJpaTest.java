package io.github.rxtcp.integrationcheck.repository;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.rxtcp.integrationcheck.repository.RepoTestFixtures.anyCheckType;
import static io.github.rxtcp.integrationcheck.repository.RepoTestFixtures.newCheck;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("CheckRepository: JPA-поведение и SQL-контракты")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckRepositoryDataJpaTest {

    private static final long UNKNOWN_ID = 999_999L;

    // Валидные значения по умолчанию для RestApiProfile (соответствуют аннотациям в entity)
    private static final String DEFAULT_URL = "https://example.org/health";
    private static final int DEFAULT_TIMEOUT_SEC = 5;          // [1..600]
    private static final int DEFAULT_EXPECTED_HTTP = 200;      // [100..599]
    private static final HttpMethod DEFAULT_METHOD = HttpMethod.GET;

    @Autowired
    private CheckRepository repository;

    @Autowired
    private EntityManager em;

    /**
     * Заполняем обязательные поля RestApiProfile, если они не заданы фикстурами.
     * Это делает тест устойчивым к «пустым» профилям из RepoTestFixtures.
     */
    private static void ensureValidProfile(RestApiProfile p) {
        if (p.getUrl() == null || p.getUrl().isBlank()) {
            p.setUrl(DEFAULT_URL);
        }
        if (p.getHttpMethod() == null) {
            p.setHttpMethod(DEFAULT_METHOD);
        }
        if (p.getTimeoutSeconds() < 1 || p.getTimeoutSeconds() > 600) {
            p.setTimeoutSeconds(DEFAULT_TIMEOUT_SEC);
        }
        if (p.getExpectedHttpCode() < 100 || p.getExpectedHttpCode() > 599) {
            p.setExpectedHttpCode(DEFAULT_EXPECTED_HTTP);
        }
    }

    // ===== Утилиты ============================================================================

    @AfterEach
    void cleanup_entityManager() {
        em.clear();
    }

    /**
     * Сохраняем и перегружаем сущность, чтобы работать со «свежим» состоянием из БД.
     */
    private Check persistCheck(Check check) {
        repository.saveAndFlush(check);
        em.clear();
        return em.find(Check.class, check.getId());
    }

    /**
     * Сохраняем чек вместе с профилем. Перед привязкой приводим профиль к валидному состоянию,
     * чтобы не падать на Bean Validation при каскадном сохранении.
     */
    private Check persistCheckWithProfile(Check check, RestApiProfile profile) {
        ensureValidProfile(profile);
        check.attachProfile(profile, anyCheckType());
        repository.saveAndFlush(check);
        em.clear();
        return em.find(Check.class, check.getId());
    }

    // ===== findDueCheckIds =====================================================================

    @Nested
    @DisplayName("findDueCheckIds()")
    class FindDueCheckIds {

        @Test
        void should_return_only_enabled_and_due() {
            final LocalDateTime now = LocalDateTime.now();

            final Check dueEnabled = persistCheck(newCheck("due", true, now.minusHours(2)));
            final Check futureEnabled = persistCheck(newCheck("future", true, now.plusHours(2)));
            final Check pastDisabled = persistCheck(newCheck("disabled", false, now.minusHours(2)));

            final List<Long> ids = repository.findDueCheckIds();

            assertThat(ids).contains(dueEnabled.getId());
            assertThat(ids).doesNotContain(futureEnabled.getId(), pastDisabled.getId());
        }

        @Test
        void should_include_boundary_when_nextRunAt_is_equal_or_before_now() {
            final LocalDateTime almostNow = LocalDateTime.now().minusSeconds(1);
            final Check due = persistCheck(newCheck("boundary", true, almostNow));

            final List<Long> ids = repository.findDueCheckIds();

            assertThat(ids).contains(due.getId());
        }


        @Test
        @Sql(
                executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
                // ВАЖНО: сначала чистим дочерние таблицы, потом родительскую
                statements = {
                        "delete from integration_health_check.h_check_rest_api",
                        "delete from integration_health_check.h_check_profile",
                        "delete from integration_health_check.h_check"
                }
        )
        void should_return_empty_when_none_due() {
            final LocalDateTime now = LocalDateTime.now();

            persistCheck(newCheck("a", false, now.minusHours(1)));
            persistCheck(newCheck("b", true, now.plusHours(5)));

            final List<Long> ids = repository.findDueCheckIds();

            assertThat(ids).isEmpty();
        }
    }

    // ===== findWithProfileById =================================================================

    @Nested
    @DisplayName("findWithProfileById(id)")
    class FindWithProfileById {

        @Test
        void should_fetch_profile_and_owner_consistently() {
            final Check check = newCheck("with-prof", true, LocalDateTime.now().minusHours(1));
            final RestApiProfile profile = new RestApiProfile(); // может быть «пустой» из фикстур
            final Check saved = persistCheckWithProfile(check, profile);

            final Optional<Check> opt = repository.findWithProfileById(saved.getId());

            assertThat(opt).isPresent();
            final Check found = opt.orElseThrow();

            assertThat(found.getId()).isEqualTo(saved.getId());
            assertThat(found.getProfile()).isNotNull();
            assertThat(found.getProfile().getId()).isNotNull();

            // обратная ссылка профиля должна указывать на найденный Check
            assertThat(found.getProfile().getCheck()).isSameAs(found);

            // профиль подгружён (EntityGraph/настройки fetch)
            assertThat(Hibernate.isInitialized(found.getProfile())).isTrue();
        }

        @Test
        void should_return_empty_for_unknown_id() {
            final Optional<Check> opt = repository.findWithProfileById(UNKNOWN_ID);
            assertThat(opt).isEmpty();
        }

        @Test
        void should_return_entity_with_null_profile_when_check_has_no_profile() {
            final Check saved = persistCheck(newCheck("no-prof", true, LocalDateTime.now().minusMinutes(30)));

            final Optional<Check> opt = repository.findWithProfileById(saved.getId());

            assertThat(opt).isPresent();
            assertThat(opt.orElseThrow().getProfile()).isNull();
        }
    }

    // ===== Уникальность имени ===================================================================

    @Nested
    @DisplayName("Ограничения уникальности")
    class UniqueConstraints {

        @Test
        void should_fail_on_duplicate_name_on_save_or_flush() {
            final LocalDateTime now = LocalDateTime.now();

            repository.saveAndFlush(newCheck("unique-name", true, now.minusMinutes(1)));

            final Check duplicate = newCheck("unique-name", true, now.minusMinutes(2));

            assertThatThrownBy(() -> {
                repository.save(duplicate);
                repository.flush();
            }).isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ===== CRUD roundtrip ======================================================================

    @Nested
    @DisplayName("CRUD roundtrip")
    class CrudRoundtrip {

        @Test
        void should_create_read_update_delete_entity() {
            final Check toSave = newCheck("crud", true, LocalDateTime.now().minusMinutes(1));

            final Check saved = repository.save(toSave);
            assertThat(saved.getId()).isNotNull();

            final Check reloaded = repository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getName()).isEqualTo("crud");

            reloaded.setEnabled(false);
            repository.saveAndFlush(reloaded);
            final Check afterUpdate = repository.findById(saved.getId()).orElseThrow();
            assertThat(afterUpdate.isEnabled()).isFalse();

            repository.delete(afterUpdate);
            repository.flush();
            assertThat(repository.findById(saved.getId())).isEmpty();
        }
    }
}