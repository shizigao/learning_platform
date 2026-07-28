/* 文件职责：定义管理学习资料列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.content.domain.ContentStatus;
import jakarta.validation.constraints.Size;

/**
 * 定义管理学习资料列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class AdminContentListQuery extends PageQuery {
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ContentStatus status;

    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    /** 保存keyword，供该类型的业务逻辑读取或更新。 */
    private String keyword;

    /** 返回状态。 */
    public ContentStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    /** 返回Keyword。 */
    public String getKeyword() {
        return keyword;
    }

    /** 更新Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
