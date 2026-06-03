package ru.stepanov.selfcontrol.api.contract;

/**
 * Метаданные пагинации (0-based page) по REST_КОНТРАКТ.yaml.
 */
public record PageMeta(int page, int size, long totalElements, int totalPages) {
}
