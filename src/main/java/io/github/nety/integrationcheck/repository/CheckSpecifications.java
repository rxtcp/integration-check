package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckLock;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Subquery;
import java.time.OffsetDateTime;
import java.util.Set;

public final class CheckSpecifications {
    private CheckSpecifications() {}

    /** is_enabled = true */
    public static Specification<Check> enabled() {
        return (root, query, cb) -> cb.isTrue(root.get("enabled"));
    }

    /** next_run_at <= :ts */
    public static Specification<Check> dueAtOrBefore(OffsetDateTime ts) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("nextRunAt"), ts);
    }

    /**
     * NOT EXISTS активной блокировки для clusterId:
     * NOT EXISTS (select 1 from h_check_lock l
     *             where l.check = root and l.clusterId = :clusterId and l.lockedUntil > :now)
     */
    public static Specification<Check> notLockedForCluster(String clusterId, OffsetDateTime now) {
        return (root, query, cb) -> {
            Subquery<Long> sq = query.subquery(Long.class);
            var lock = sq.from(CheckLock.class);
            sq.select(cb.literal(1L))
                    .where(
                            cb.equal(lock.get("check"), root),
                            cb.equal(lock.get("id").get("clusterId"), clusterId),
                            cb.greaterThan(lock.get("lockedUntil"), now)
                    );
            return cb.not(cb.exists(sq));
        };
    }

    /** type_code IN (...); пустой набор — no-op спецификация */
    public static Specification<Check> typeCodeIn(Set<String> typeCodes) {
        if (typeCodes == null || typeCodes.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get("typeCode").in(typeCodes);
    }

    /** Композит для удобства сборки набора условий */
    public static Specification<Check> allDueForCluster(String clusterId, OffsetDateTime now, Set<String> typeCodes) {
        return Specification.allOf(
                enabled(),
                dueAtOrBefore(now),
                notLockedForCluster(clusterId, now),
                typeCodeIn(typeCodes) // вернёт unrestricted(), если фильтр не задан
        );
    }
}

