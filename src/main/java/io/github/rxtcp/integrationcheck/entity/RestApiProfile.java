package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.entity.support.HibernateEntityUtil;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Профиль проверки REST API (подтип {@link CheckProfile}); JOINED, дискриминатор {@code REST_API}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "h_check_rest_api")
@DiscriminatorValue("REST_API")
@PrimaryKeyJoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_h_check_rest_api__profile"))
public class RestApiProfile extends CheckProfile {

    /**
     * Целевой URL.
     */
    @NotBlank
    @Column(name = "url", nullable = false)
    private String url;

    /** HTTP-метод запроса. */
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method_code", nullable = false, length = 10)
    private HttpMethod httpMethod;

    /**
     * Таймаут, сек (1–600); по умолчанию 30.
     */
    @Min(1)
    @Max(600)
    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 30;

    /**
     * Заголовки запроса (JSON).
     */
    @Column(name = "headers")
    private String headers;

    /** Тело запроса (опционально). */
    @Column(name = "request_body")
    private String requestBody;

    /**
     * Ожидаемый HTTP-код ответа (100–599).
     */
    @Min(100)
    @Max(599)
    @Column(name = "expected_http_code", nullable = false)
    private int expectedHttpCode;

    /**
     * Диагностический вывод.
     */
    @Override
    public String toString() {
        return "RestApiProfile{" +
                "id=" + getId() +
                ", type=" + getType() +
                ", checkId=" + HibernateEntityUtil.idOf(getCheck()) +
                ", url='" + url + '\'' +
                ", httpMethod=" + httpMethod +
                ", timeoutSeconds=" + timeoutSeconds +
                ", headers='" + headers + '\'' +
                ", requestBody='" + requestBody + '\'' +
                ", expectedHttpCode=" + expectedHttpCode +
                '}';
    }
}
