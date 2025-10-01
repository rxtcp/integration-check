package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.common.contract.Identifiable;
import io.github.rxtcp.integrationcheck.entity.support.HibernateEntityUtil;
import io.github.rxtcp.integrationcheck.domain.CheckRunStatus;
import io.github.rxtcp.integrationcheck.domain.FailureReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

/**
 * Результат выполнения проверки.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "h_check_result")
public class CheckResult implements Identifiable<Long> {

    /**
     * PK.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Проверка (LAZY); может быть null для сохранения истории.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id")
    private Check check;

    /**
     * Время старта.
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * Время завершения.
     */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    /**
     * Статус выполнения.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CheckRunStatus status;

    /**
     * Причина неуспеха (если есть).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason")
    private FailureReason failureReason;

    /**
     * Детали результата/ошибки.
     */
    @Column(name = "details")
    private String details;

    /**
     * Равенство по id с учётом прокси Hibernate; без id не равны.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        Long thisId = HibernateEntityUtil.idOf(this);
        Long otherId = HibernateEntityUtil.idOf(o);
        return thisId != null && thisId.equals(otherId);
    }

    /**
     * Хеш по фактическому классу Hibernate.
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    /**
     * Диагностическое представление.
     */
    @Override
    public String toString() {
        return "CheckResult{" +
                "id=" + id +
                ", checkId=" + HibernateEntityUtil.idOf(check) +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", status=" + status +
                ", failureReason=" + failureReason +
                ", details='" + details + '\'' +
                '}';
    }
}