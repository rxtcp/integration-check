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
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "h_check_http",
        indexes = {
                @Index(name = "ix_h_check_http__check_id", columnList = "check_id")
        })
@Comment("Параметры для проверок REST API")
public class CheckHttp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_h_check_http__check_type"))
    @Comment("FK на h_check.id; каскадное удаление на уровне БД")
    private Check check;

    @NotBlank
    @Column(nullable = false)
    @Comment("Целевой URL")
    private String url;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method_code", nullable = false)
    @Comment("HTTP-метод")
    private HttpMethod httpMethod;

    @Min(1)
    @Max(600)
    @NotNull
    @Column(name = "timeout_seconds", nullable = false)
    @Comment("Таймаут HTTP-запроса, сек")
    private Integer timeoutSeconds = 30;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Comment("Заголовки запроса (JSON-объект)")
    private Map<String, String> headers;

    @Lob
    @Comment("Шаблон тела запроса")
    private String requestBody;

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CheckHttp checkHttp)) return false;
        return Objects.equals(id, checkHttp.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
