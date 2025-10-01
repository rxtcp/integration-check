package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.common.contract.Identifiable;
import io.github.rxtcp.integrationcheck.entity.support.HibernateEntityUtil;
import io.github.rxtcp.integrationcheck.enums.CheckType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

/**
 * Проверка интеграции (сущность планировщика). 1:1 профиль, уникальное имя.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "h_check",
        uniqueConstraints = @UniqueConstraint(name = "uq_h_check__name", columnNames = "name")
)
public class Check implements Identifiable<Long> {

    /**
     * Идентификатор (PK).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Уникальное имя проверки (<=250).
     */
    @NotBlank
    @Column(name = "name", nullable = false, length = 250)
    private String name;

    /**
     * Описание (<=1000).
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Включена в планировщике.
     */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Интервал запусков, мин (1–10080).
     */
    @Min(1)
    @Max(10080)
    @Column(name = "run_interval_min", nullable = false)
    private int runIntervalMin;

    /**
     * Время следующего запуска.
     */
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    /**
     * Тип проверки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false)
    private CheckType type;

    /**
     * Профиль проверки.
     */
    @OneToOne(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true)
    private CheckProfile profile;

    /**
     * Привязать профиль и установить тип.
     * Устанавливает обратную ссылку и возвращает профиль.
     */
    public <P extends CheckProfile> P attachProfile(P profile, CheckType type) {
        this.type = type;
        this.profile = profile;
        profile.setCheck(this);
        return profile;
    }

    /**
     * Равенство по идентификатору с учётом прокси Hibernate.
     * Сущности без id не равны.
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
     * Хеш-код по фактическому классу Hibernate (совместим с equals).
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    /**
     * Диагностический вывод, включает profileId.
     */
    @Override
    public String toString() {
        return "Check{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", enabled=" + enabled +
                ", runIntervalMin=" + runIntervalMin +
                ", nextRunAt=" + nextRunAt +
                ", type=" + type +
                ", profileId=" + HibernateEntityUtil.idOf(profile) +
                '}';
    }
}