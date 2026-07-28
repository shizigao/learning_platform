/* 文件职责：定义分页查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：分页基础模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 定义分页查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：遵守 统一协议、异常、配置与跨领域基础设施 模块的职责边界。</p>
 */
public class PageQuery {
    @Min(value = 1, message = "页码必须大于等于 1")
    /** 保存分页Number，供该类型的业务逻辑读取或更新。 */
    private int pageNumber = 1;

    @Min(value = 1, message = "每页数量必须大于等于 1")
    @Max(value = 100, message = "每页数量不能超过 100")
    /** 保存分页Size，供该类型的业务逻辑读取或更新。 */
    private int pageSize = 20;

    /** 返回分页Number。 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** 更新分页Number；调用方仍需遵守所属领域的校验规则。 */
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    /** 返回分页Size。 */
    public int getPageSize() {
        return pageSize;
    }

    /** 更新分页Size；调用方仍需遵守所属领域的校验规则。 */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /** 转换或规范化fset数据，不引入额外持久化副作用。 */
    public long offset() {
        return (long) (pageNumber - 1) * pageSize;
    }
}

