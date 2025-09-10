package io.github.nety.integrationcheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CheckLockId implements Serializable {

    @Column(name = "check_id", nullable = false)
    private Long checkId;

    @Column(name = "cluster_id", nullable = false)
    private String clusterId;

    public CheckLockId() {
    }

    public CheckLockId(Long checkId, String clusterId) {
        this.checkId = checkId;
        this.clusterId = clusterId;
    }

    public Long getCheckId() {
        return checkId;
    }

    public String getClusterId() {
        return clusterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CheckLockId that)) return false;
        return Objects.equals(checkId, that.checkId) &&
                Objects.equals(clusterId, that.clusterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkId, clusterId);
    }
}