/* 文件职责：定义教师申请管理查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.offline.domain.TeacherApplicationStatus;
import jakarta.validation.constraints.Size;

/**
 * 定义教师申请管理查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class TeacherApplicationAdminQuery extends PageQuery {
    @Size(max = 100)
    /** 保存keyword，供该类型的业务逻辑读取或更新。 */
    private String keyword;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private TeacherApplicationStatus status;

    /** 返回Keyword。 */
    public String getKeyword() { return keyword; }
    /** 更新Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setKeyword(String value) {
        keyword = value == null || value.isBlank() ? null : value.trim();
    }
    /** 返回状态。 */
    public TeacherApplicationStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(TeacherApplicationStatus value) { status = value; }
}
