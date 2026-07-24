package com.learningplatform.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageQuery {
    @Min(value = 1, message = "页码必须大于等于 1")
    private int pageNumber = 1;

    @Min(value = 1, message = "每页数量必须大于等于 1")
    @Max(value = 100, message = "每页数量不能超过 100")
    private int pageSize = 20;

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long offset() {
        return (long) (pageNumber - 1) * pageSize;
    }
}

