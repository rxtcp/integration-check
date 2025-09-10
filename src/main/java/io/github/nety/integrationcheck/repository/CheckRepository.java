package io.github.nety.integrationcheck.repository;

import io.github.nety.integrationcheck.domain.Check;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface CheckRepository extends JpaRepository<Check, Long>, JpaSpecificationExecutor<Check> {

    /** Топ-N «должных к запуску» проверок, сортировка по nextRunAt ↑ */
    default List<Check> findDueChecks(String clusterId,
                                      OffsetDateTime now,
                                      Set<String> typeCodes,
                                      int limit) {
        var spec = CheckSpecifications.allDueForCluster(clusterId, now, typeCodes);
        return this.findBy(spec, q -> q
                .sortBy(Sort.by(Sort.Direction.ASC, "nextRunAt"))
                .limit(Math.max(1, limit))
                .all());
    }

    /** Пагинированный вариант с гарантированной сортировкой по nextRunAt ↑ */
    default Page<Check> findDueChecksPage(String clusterId,
                                          OffsetDateTime now,
                                          Set<String> typeCodes,
                                          Pageable pageable) {
        var spec = CheckSpecifications.allDueForCluster(clusterId, now, typeCodes);
        var sort = pageable.getSort().isUnsorted()
                ? Sort.by(Sort.Direction.ASC, "nextRunAt")
                : pageable.getSort();

        return this.findBy(spec, q -> q
                .sortBy(sort)
                .page(PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)));
    }

    /** Когда нужно сразу подтянуть httpParams (избежать N+1) */
    @EntityGraph(attributePaths = "httpParams")
    Page<Check> findAll(Specification<Check> spec, Pageable pageable);
}
