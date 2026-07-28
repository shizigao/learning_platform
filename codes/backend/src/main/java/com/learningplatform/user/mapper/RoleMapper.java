/* 文件职责：定义角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.Role;
import com.learningplatform.user.domain.RoleCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface RoleMapper {

    @Select("""
            SELECT id, code, name, description, enabled, created_at, updated_at
            FROM `role`
            WHERE code = #{code} AND enabled = 1
            """)
    /** 执行 findEnabledByCode 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<Role> findEnabledByCode(RoleCode code);

    @Select("""
            SELECT r.id, r.code, r.name, r.description, r.enabled, r.created_at, r.updated_at
            FROM `role` r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.enabled = 1
            ORDER BY r.id
            """)
    /** 执行 findEnabledByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Role> findEnabledByUserId(Long userId);
}
