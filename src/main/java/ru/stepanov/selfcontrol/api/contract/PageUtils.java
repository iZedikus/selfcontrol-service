package ru.stepanov.selfcontrol.api.contract;

import org.springframework.data.domain.Page;

/**
 * Сборка пагинированных ответов по REST_КОНТРАКТ.yaml.
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }
}
