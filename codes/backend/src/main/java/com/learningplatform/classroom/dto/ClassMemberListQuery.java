/* 文件职责：定义班级成员列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Size;

/**
 * 定义班级成员列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class ClassMemberListQuery extends PageQuery {
    @Size(max = 100, message = "成员搜索关键字不能超过100个字符")
    /** 保存keyword，供该类型的业务逻辑读取或更新。 */
    private String keyword;

    /** 返回Keyword。 */
    public String getKeyword() { return keyword; }
    /** 更新Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
