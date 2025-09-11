package io.github.nety.integrationcheck.cluster;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClusterSingletonGuard {

    private static final int NAMESPACE = 42; // любой фиксированный "неймспейс" для lock
    private final JdbcTemplate jdbc;

    public boolean isActiveFor(String jobName, String nodeId) {
        Boolean active = jdbc.queryForObject(
                "select (enabled and owner_id = ?) from batch_singleton where job_name = ?",
                Boolean.class, nodeId, jobName
        );
        return Boolean.TRUE.equals(active);
    }

    public boolean tryAdvisoryLock(String jobName) {
        int key = jobName.hashCode();
        Boolean locked = jdbc.queryForObject(
                "select pg_try_advisory_lock(?, ?)",
                Boolean.class, NAMESPACE, key
        );
        return Boolean.TRUE.equals(locked);
    }

    public void releaseAdvisoryLock(String jobName) {
        int key = jobName.hashCode();
        jdbc.update("select pg_advisory_unlock(?, ?)", NAMESPACE, key);
    }
}
