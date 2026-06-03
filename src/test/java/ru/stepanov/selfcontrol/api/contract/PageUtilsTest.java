package ru.stepanov.selfcontrol.api.contract;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageUtilsTest {

    @Test
    void mapsSpringPageToPagedResponse() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 10), 25);

        PagedResponse<String> response = PageUtils.from(page);

        assertEquals(List.of("a", "b"), response.content());
        assertEquals(1, response.meta().page());
        assertEquals(10, response.meta().size());
        assertEquals(25, response.meta().totalElements());
        assertEquals(3, response.meta().totalPages());
    }
}
