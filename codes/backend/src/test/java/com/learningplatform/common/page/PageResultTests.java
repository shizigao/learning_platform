package com.learningplatform.common.page;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTests {

    @Test
    void calculatesTotalPages() {
        PageResult<String> result = PageResult.of(List.of("a", "b"), 21, 2, 10);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.items()).containsExactly("a", "b");
    }

    @Test
    void returnsZeroPagesForEmptyResult() {
        PageResult<String> result = PageResult.of(List.of(), 0, 1, 20);

        assertThat(result.totalPages()).isZero();
    }
}

