package com.gordeev.bankcards.dto.api;

import java.util.List;

public record PageResponse<T>(
        List<T> entities,

        Metadata meta
) {
    public record Metadata(
            long size,
            long totalElements,
            long totalPages,
            long currentPageNumber
    ) {}
}
