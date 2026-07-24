package com.learningplatform.common.page;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        long total,
        int pageNumber,
        int pageSize,
        long totalPages
) {
    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, long total, int pageNumber, int pageSize) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(items, total, pageNumber, pageSize, totalPages);
    }
}

