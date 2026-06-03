package ru.stepanov.selfcontrol.api.contract;

import java.util.List;

/**
 * Пагинированный ответ: {@code content} + {@code meta} по REST_КОНТРАКТ.yaml.
 */
public record PagedResponse<T>(List<T> content, PageMeta meta) {
}
