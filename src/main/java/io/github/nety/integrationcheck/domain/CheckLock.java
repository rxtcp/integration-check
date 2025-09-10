package io.github.nety.integrationcheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

@Entity
@Table(name = "h_check_lock",
        indexes = {
                @Index(name = "ix_h_check_lock__locked_until", columnList = "locked_until")
        })
@Comment("Блокировка выполнения проверки внутри кластера (check_id + cluster_id)")
public class CheckLock {

    @EmbeddedId
    private CheckLockId id;

    @MapsId("checkId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id",
            foreignKey = @ForeignKey(name = "fk_h_check_lock__check"))
    private Check check;

    @NotNull
    @Column(name = "locked_until", nullable = false)
    @Comment("Момент истечения блокировки; после — блокировка может быть перехвачена")
    private OffsetDateTime lockedUntil;

    public CheckLock() {
    }

    public CheckLock(Check check, String clusterId, OffsetDateTime lockedUntil) {
        this.check = check;
        this.id = new CheckLockId(check.getId(), clusterId);
        this.lockedUntil = lockedUntil;
    }

    // -------- Getters/Setters --------

    public CheckLockId getId() {
        return id;
    }

    public void setId(CheckLockId id) {
        this.id = id;
    }

    public Check getCheck() {
        return check;
    }

    public void setCheck(Check check) {
        this.check = check;
        if (this.id == null) {
            this.id = new CheckLockId();
        }
        // при связывании после persist() у check уже есть id
        this.id = new CheckLockId(check != null ? check.getId() : null,
                this.id != null ? this.id.getClusterId() : null);
    }

    public String getClusterId() {
        return id != null ? id.getClusterId() : null;
    }

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }
}