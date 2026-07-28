/* 文件职责：定义用户角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
/**
 * 定义用户角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface UserRoleMapper {

    @Select("""
            SELECT COUNT(*) > 0
            FROM user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    /** 执行 exists 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean exists(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Insert("""
            INSERT INTO user_role (user_id, role_id, granted_by)
            VALUES (#{userId}, #{roleId}, #{grantedBy})
            """)
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(UserRole userRole);

    @Delete("""
            DELETE FROM user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    /** 执行 delete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int delete(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
