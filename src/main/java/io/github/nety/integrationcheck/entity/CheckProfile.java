package io.github.nety.integrationcheck.entity;

import io.github.nety.integrationcheck.common.contract.Identifiable;
import io.github.nety.integrationcheck.entity.support.HibernateEntityUtil;
import io.github.nety.integrationcheck.enums.CheckType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

/**
 * Базовый профиль проверки. JOINED-наследование, дискриминатор {@code type_code}, связь 1:1 с {@link Check}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "h_check_profile",
        uniqueConstraints = @UniqueConstraint(name = "uq_h_check_profile__check_id", columnNames = "check_id")
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_code")
public abstract class CheckProfile implements Identifiable<Long> {

    /**
     * Идентификатор профиля (PK).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Проверка-владелец (обязательная, LAZY).
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id", nullable = false, foreignKey = @ForeignKey(name = "fk_h_check_profile__check"))
    private Check check;

    /**
     * Тип профиля; заполняется дискриминатором (read-only).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", insertable = false, updatable = false, nullable = false)
    private CheckType type;

    /**
     * Равенство по id с учётом прокси Hibernate; сущности без id не равны.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) return false;

        Long thisId = HibernateEntityUtil.idOf(this);
        Long otherId = HibernateEntityUtil.idOf(o);
        return thisId != null && thisId.equals(otherId);
    }

    /**
     * Хеш по фактическому классу Hibernate (совместим с equals).
     */
    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

    /**
     * Диагностическое представление: id, type, checkId.
     */
    @Override
    public String toString() {
        return HibernateEntityUtil.simpleClassName(this) +
                "{id=" + getId() +
                ", type=" + getType() +
                ", checkId=" + HibernateEntityUtil.idOf(getCheck()) +
                '}';
    }
}
