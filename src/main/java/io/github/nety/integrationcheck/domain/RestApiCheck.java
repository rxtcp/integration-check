package io.github.nety.integrationcheck.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Lob;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "h_check_rest_api")
@DiscriminatorValue("REST_API") // соответствует коду в ref_check_type
@PrimaryKeyJoinColumn(
        name = "check_id",
        foreignKey = @ForeignKey(name = "fk_h_check_rest_api__check")
)
public class RestApiCheck extends Check {

    @NotBlank
    @Column(nullable = false)
    private String url;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method_code", nullable = false, length = 10)
    private HttpMethod httpMethod;

    @Min(1) @Max(600)
    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 30;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> headers = Map.of();

    @Lob
    @Column(name = "request_body")
    private String requestBody;
}
