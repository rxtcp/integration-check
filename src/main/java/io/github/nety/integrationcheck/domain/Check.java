package io.github.nety.integrationcheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "h_check",
        uniqueConstraints = @UniqueConstraint(name = "uq_h_check__name", columnNames = "name"),
        indexes = @Index(name = "ix_h_check__due", columnList = "next_run_at")
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type_code", discriminatorType = DiscriminatorType.STRING)
public abstract class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250)
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @NotNull
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = Boolean.TRUE;

    @Min(1)
    @Max(10080)
    @NotNull
    @Column(name = "run_interval_min", nullable = false)
    private Integer runIntervalMinutes;

    @NotNull
    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Check other = (Check) o;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
