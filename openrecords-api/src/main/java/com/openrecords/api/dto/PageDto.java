package com.openrecords.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Clean JSON wrapper for paginated responses.
 *
 * Maps to responses like:
 * {
 *   "content": [ ... ],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 143,
 *   "totalPages": 8
 * }
 *
 * Built from Spring Data's Page<T> via the static factory method.
 */
public record PageDto<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    /**
     * Convert a Spring Data Page of entities into a PageDto of DTOs,
     * using the given mapper function.
     *
     * Example:
     *   PageDto<FoiaRequestDto> dto = PageDto.from(requestPage, mapper::toDto);
     */
    public static <E, D> PageDto<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageDto<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}