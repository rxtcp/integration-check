package io.github.rxtcp.integrationcheck.repository;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.rxtcp.integrationcheck.repository.ResultRepoFixtures.newCheck;
import static io.github.rxtcp.integrationcheck.repository.ResultRepoFixtures.newResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты для {@link CheckResultRepository} на уровне JPA/БД.
 * <p>
 * Покрываем:
 * - базовый CRUD и ленивые связи;
 * - ограничения Bean Validation и БД;
 * - пагинацию/сортировку;
 * - batch-операции.
 */
@ActiveProfiles("test")
@DataJpaTest
@DisplayName("CheckResultRepository: поведение JPA и SQL-контрактов")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultRepositoryDataJpaTest {

    private static final int PAGE_SIZE = 2;

    @Autowired
    CheckResultRepository repository;

    @Autowired
    EntityManager em;

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    // === CRUD и связи =====================================================================================

    @AfterEach
    void clear_entity_manager() {
        em.clear();
    }

    // === Валидация и ограничения БД =======================================================================

    @Nested
    @DisplayName("CRUD и ассоциации")
    class CrudAndAssociations {

        @Test
        void should_save_and_findById_when_check_is_null() {
            // given
            final CheckResult result = newResult();

            // when
            final CheckResult saved = repository.saveAndFlush(result);

            // then
            assertThat(saved.getId()).isNotNull();

            final CheckResult found = repository.findById(saved.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo(result.getStatus());
            assertThat(found.getCheck()).isNull();
            assertThat(found.getStartedAt()).isNotNull();
            assertThat(found.getFinishedAt()).isNotNull();
        }

        @Test
        void should_link_existing_check_and_keep_lazy_proxy_uninitialized_until_explicit_initialize() {
            // given: Check сохраняем отдельно (нет каскада)
            final Check check = newCheck("c1");
            em.persist(check);

            final CheckResult result = newResult();
            result.setCheck(check);

            // when
            repository.saveAndFlush(result);
            em.clear(); // гарантируем загрузку заново

            final CheckResult found = repository.findById(result.getId()).orElseThrow();

            // then: доступ к id не инициализирует LAZY-прокси
            assertThat(found.getCheck().getId()).isEqualTo(check.getId());
            assertThat(Hibernate.isInitialized(found.getCheck())).isFalse();

            // инициализируем явно
            Hibernate.initialize(found.getCheck());
            assertThat(Hibernate.isInitialized(found.getCheck())).isTrue();
        }

        @Test
        void should_update_and_delete_entity() {
            // given
            final CheckResult saved = repository.saveAndFlush(newResult());

            // update
            saved.setDetails("updated");
            repository.saveAndFlush(saved);

            final CheckResult reloaded = repository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getDetails()).isEqualTo("updated");

            // delete
            repository.delete(reloaded);
            repository.flush();

            assertThat(repository.findById(saved.getId())).isEmpty();
        }
    }

    // === Пагинация и сортировка ===========================================================================

    @Nested
    @DisplayName("Ограничения Bean Validation и БД")
    class Constraints {

        @Test
        void should_throw_constraint_violation_when_status_is_null() {
            final CheckResult r = newResult();
            r.setStatus(null);

            assertThatThrownBy(() -> repository.saveAndFlush(r))
                    .isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_enforce_db_not_null_on_startedAt() {
            final CheckResult r = newResult();
            r.setStartedAt(null);

            assertThatThrownBy(() -> repository.saveAndFlush(r))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void should_allow_nullable_finishedAt_and_persist_null_value() {
            final CheckResult r = newResult();
            r.setFinishedAt(null);

            final CheckResult saved = repository.saveAndFlush(r);

            assertThat(saved.getId()).isNotNull();
            final CheckResult reloaded = repository.findById(saved.getId()).orElseThrow();
            assertThat(reloaded.getFinishedAt()).isNull();
        }

        @Test
        void should_throw_for_transient_check_reference() {
            final Check transientCheck = newCheck("transient"); // не сохраняем
            final CheckResult r = newResult();
            r.setCheck(transientCheck);

            assertThatThrownBy(() -> repository.saveAndFlush(r))
                    .isInstanceOfAny(InvalidDataAccessApiUsageException.class, PersistenceException.class)
                    .satisfies(ex -> {
                        final Throwable root = rootCause(ex);
                        // допускаем обе Hibernate-ошибки, в зависимости от пути
                        assertThat(root.getClass().getName())
                                .isIn(
                                        "org.hibernate.TransientObjectException",
                                        "org.hibernate.TransientPropertyValueException"
                                );
                    });
        }
    }

    // === Batch-операции ===================================================================================

    @Nested
    @DisplayName("Пагинация и сортировка")
    class PagingAndSorting {

        @Test
        void should_page_and_sort_by_startedAt_desc() {
            final CheckResult r1 = newResult();
            r1.setStartedAt(LocalDateTime.now().minusMinutes(3));
            final CheckResult r2 = newResult();
            r2.setStartedAt(LocalDateTime.now().minusMinutes(2));
            final CheckResult r3 = newResult();
            r3.setStartedAt(LocalDateTime.now().minusMinutes(1));
            repository.saveAllAndFlush(List.of(r1, r2, r3));

            final Page<CheckResult> page = repository.findAll(
                    PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "startedAt"))
            );

            assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
            assertThat(page.getContent()).hasSize(PAGE_SIZE);
            assertThat(page.getContent().get(0).getStartedAt())
                    .isAfterOrEqualTo(page.getContent().get(1).getStartedAt());
        }
    }

    // === Вспомогательное ================================================================================

    @Nested
    @DisplayName("Batch-операции")
    class BatchOperations {

        @Test
        void should_delete_all_in_batch_and_reduce_count_to_zero() {
            final long before = repository.count();

            repository.saveAllAndFlush(List.of(newResult(), newResult(), newResult()));
            final long mid = repository.count();

            final var all = repository.findAll();
            repository.deleteAllInBatch(all);
            repository.flush();

            final long after = repository.count();
            assertThat(mid).isGreaterThan(before);
            assertThat(after).isZero();
        }
    }
}