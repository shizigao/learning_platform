/* 文件职责：定义学习资料Reference搜索查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 定义学习资料Reference搜索查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class ContentReferenceSearchQuery extends PageQuery {
    @Size(max = 100, message = "资料名称搜索关键词不能超过100个字符")
    /** 保存标题Keyword，供该类型的业务逻辑读取或更新。 */
    private String titleKeyword;

    @Size(max = 100, message = "发布者名字搜索关键词不能超过100个字符")
    /** 保存发布者Keyword，供该类型的业务逻辑读取或更新。 */
    private String publisherKeyword;

    @Min(value = 1, message = "排除的资料ID必须为正数")
    /** 保存exclude学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long excludeContentId;

    /** 返回标题Keyword。 */
    public String getTitleKeyword() {
        return titleKeyword;
    }

    /** 更新标题Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setTitleKeyword(String titleKeyword) {
        this.titleKeyword = titleKeyword;
    }

    /** 返回发布者Keyword。 */
    public String getPublisherKeyword() {
        return publisherKeyword;
    }

    /** 更新发布者Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setPublisherKeyword(String publisherKeyword) {
        this.publisherKeyword = publisherKeyword;
    }

    /** 返回Exclude学习资料ID。 */
    public Long getExcludeContentId() {
        return excludeContentId;
    }

    /** 更新Exclude学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setExcludeContentId(Long excludeContentId) {
        this.excludeContentId = excludeContentId;
    }
}
