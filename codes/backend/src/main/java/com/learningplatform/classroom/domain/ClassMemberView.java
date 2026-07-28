/* 文件职责：表示班级成员浏览领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.domain;

import java.time.LocalDateTime;

/**
 * 表示班级成员浏览领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ClassMemberView extends ClassMember {
    /** 保存username，供该类型的业务逻辑读取或更新。 */
    private String username;
    /** 保存nickname，供该类型的业务逻辑读取或更新。 */
    private String nickname;

    /** 返回Username。 */
    public String getUsername() { return username; }
    /** 更新Username；调用方仍需遵守所属领域的校验规则。 */
    public void setUsername(String username) { this.username = username; }
    /** 返回Nickname。 */
    public String getNickname() { return nickname; }
    /** 更新Nickname；调用方仍需遵守所属领域的校验规则。 */
    public void setNickname(String nickname) { this.nickname = nickname; }
}
