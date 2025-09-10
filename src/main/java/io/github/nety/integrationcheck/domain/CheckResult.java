package io.github.nety.integrationcheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Comment;

import java.time.OffsetDateTime;

@Entity
@Table(name = "h_check_result", indexes = {@Index(name = "ix_h_check_result__check_time", columnList = "check_id, started_at DESC")})
@Comment("История запусков проверок")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // check_id может быть NULL (SET NULL при удалении проверки)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_id", foreignKey = @ForeignKey(name = "fk_h_check_result__check"))
    @Comment("FK на проверку (может быть NULL для общесистемных событий)")
    private Check check;

    @NotBlank
    @Column(name = "cluster_id", nullable = false)
    @Comment("Идентификатор кластера/окружения")
    private String clusterId;

    @NotNull
    @Column(name = "started_at", nullable = false)
    @Comment("Время начала выполнения")
    private OffsetDateTime startedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_code", nullable = false)
    @Comment("Статус результата: SUCCESS/FAILURE/TIMEOUT/ERROR")
    private ResultStatus statusCode;

    @NotNull
    @Column(name = "is_success", nullable = false)
    @Comment("Производное булево (TRUE при status_code = SUCCESS)")
    private Boolean success;

    @Lob
    @Comment("Детали выполнения/ответа (JSON как текст)")
    private String details;

    @Size(max = 100)
    @Column(name = "error_code", length = 100)
    @Comment("Код ошибки/исключения")
    private String errorCode;

    @Lob
    @Comment("Краткое сообщение об ошибке")
    private String errorMessage;

    @Min(100)
    @Max(599)
    @Column(name = "http_status_code")
    @Comment("HTTP-статус (для URL-проверок)")
    private Integer httpStatusCode;

    // -------- Helpers --------

    /**
     * Удобный сеттер, чтобы не забыть проставить is_success.
     */
    public void setStatusAndSuccess(ResultStatus status) {
        this.statusCode = status;
        this.success = (status == ResultStatus.SUCCESS);
    }

    // -------- Getters/Setters --------

    public Long getId() {
        return id;
    }

    public Check getCheck() {
        return check;
    }

    public void setCheck(Check check) {
        this.check = check;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public ResultStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(ResultStatus statusCode) {
        this.statusCode = statusCode;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }
}
