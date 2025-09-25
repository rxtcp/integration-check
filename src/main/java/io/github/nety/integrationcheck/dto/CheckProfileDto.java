package io.github.nety.integrationcheck.dto;

/**
 * Базовый профиль проверки (sealed). Используется в {@link CheckDto}. Разрешённый подтип: {@link RestApiProfileDto}.
 */
public sealed interface CheckProfileDto permits RestApiProfileDto {
}
