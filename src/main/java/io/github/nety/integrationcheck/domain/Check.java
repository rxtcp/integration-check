package io.github.nety.integrationcheck.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.NaturalId;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "h_check",
        indexes = {
                @Index(name = "ix_h_check__next_run_at", columnList = "next_run_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_h_check__name", columnNames = "name")
        })
@Comment("Список проверок (задачи планировщика)")
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Идентификатор проверки (PK)")
    private Long id;

    @NaturalId
    @NotBlank
    @Size(max = 250)
    @Column(nullable = false, length = 250)
    @Comment("Уникальное имя проверки")
    private String name;

    @Size(max = 1000)
    @Column(length = 1000)
    @Comment("Описание проверки")
    private String description;

    @NotNull
    @Column(name = "is_enabled", nullable = false)
    @Comment("Флаг включения проверки")
    private Boolean enabled = Boolean.TRUE;

    @Min(1)
    @Max(10080)
    @NotNull
    @Column(name = "run_interval_min", nullable = false)
    @Comment("Период запусков в минутах (1..10080)")
    private Integer runIntervalMin;

    @NotNull
    @Column(name = "next_run_at", nullable = false)
    @Comment("Следующее запланированное время запуска")
    private OffsetDateTime nextRunAt;

    @NotBlank
    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false)
    @Comment("Тип проверки")
    private CheckType typeCode;

    // -------- Relations --------

    @OneToMany(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Параметры HTTP-проверок; удаляются каскадно при удалении проверки")
    private Set<CheckHttp> httpParams = new LinkedHashSet<>();

    @OneToMany(mappedBy = "check", fetch = FetchType.LAZY) // без каскада: история сохраняется (SET NULL)
    @Comment("История запусков; при удалении проверки FK устанавливается в NULL")
    private Set<CheckResult> results = new LinkedHashSet<>();

    @OneToMany(mappedBy = "check", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Comment("Блокировки выполнения по кластерам")
    private Set<CheckLock> locks = new LinkedHashSet<>();

    // -------- Helpers --------

    public void addHttpParam(CheckHttp param) {
        param.setCheck(this);
        httpParams.add(param);
    }

    public void removeHttpParam(CheckHttp param) {
        param.setCheck(null);
        httpParams.remove(param);
    }

    public void addLock(CheckLock lock) {
        lock.setCheck(this);
        locks.add(lock);
    }

    public void removeLock(CheckLock lock) {
        lock.setCheck(null);
        locks.remove(lock);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Check)) return false;
        Check check = (Check) o;
        return Objects.equals(id, check.id) && Objects.equals(name, check.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
