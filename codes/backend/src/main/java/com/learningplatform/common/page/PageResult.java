/* 文件职责：以不可变记录表示分页成绩数据，并作为模块内部或接口层的数据契约。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：分页基础模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.page;

import java.util.List;

/**
 * 以不可变记录表示分页成绩数据，并作为模块内部或接口层的数据契约。
 *
 * <p>职责边界：遵守 统一协议、异常、配置与跨领域基础设施 模块的职责边界。</p>
 */
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

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static <T> PageResult<T> of(List<T> items, long total, int pageNumber, int pageSize) {
        long totalPages = total == 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(items, total, pageNumber, pageSize, totalPages);
    }
}

