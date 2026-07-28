/* 文件职责：定义用户的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.UserStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义用户的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface UserMapper {
    /** 定义 USER_COLUMNS 常量，统一该组件使用的固定规则或默认值。 */
    String USER_COLUMNS = """
            id, username, password_hash, nickname, avatar_url, email, phone, gender, bio,
            status, last_login_at, last_login_ip, created_at, updated_at, deleted
            """;

    @Select("SELECT " + USER_COLUMNS + " FROM `user` WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<User> findById(Long id);

    @Select("SELECT id FROM `user` WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    /** 执行 lockById 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    Optional<Long> lockById(Long id);

    @Select("SELECT " + USER_COLUMNS + " FROM `user` WHERE username = #{username} AND deleted = 0")
    /** 执行 findByUsername 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<User> findByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM `user` WHERE username = #{username} AND deleted = 0")
    /** 执行 existsByUsername 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean existsByUsername(String username);

    @Select("""
            SELECT
            """ + USER_COLUMNS + """
            FROM `user`
            WHERE status = 'ACTIVE' AND deleted = 0
              AND (#{keyword} IS NULL
                   OR username LIKE CONCAT('%', #{keyword}, '%')
                   OR nickname LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY username ASC, id ASC
            LIMIT 50
            """)
    /** 执行 searchActive 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<User> searchActive(String keyword);

    @Select("""
            SELECT COUNT(*)
            FROM `user`
            WHERE status = 'ACTIVE' AND deleted = 0
              AND (#{keyword} IS NULL
                   OR username LIKE CONCAT('%', #{keyword}, '%')
                   OR nickname LIKE CONCAT('%', #{keyword}, '%'))
            """)
    /** 执行 countActive 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countActive(@Param("keyword") String keyword);

    @Select("""
            SELECT
            """ + USER_COLUMNS + """
            FROM `user`
            WHERE status = 'ACTIVE' AND deleted = 0
              AND (#{keyword} IS NULL
                   OR username LIKE CONCAT('%', #{keyword}, '%')
                   OR nickname LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY username ASC, id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    /** 执行 findActivePage 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<User> findActivePage(
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM `user` u
            WHERE u.deleted = 0
            <if test='status != null'>AND u.status = #{status}</if>
            <if test='keyword != null'>
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='role != null'>
              AND EXISTS (
                SELECT 1
                FROM user_role ur
                INNER JOIN role r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND r.code = #{role} AND r.enabled = 1
              )
            </if>
            </script>
            """)
    /** 执行 countForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countForAdmin(
            @Param("status") UserStatus status,
            @Param("role") RoleCode role,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
              u.id, u.username, u.password_hash, u.nickname, u.avatar_url,
              u.email, u.phone, u.gender, u.bio, u.status, u.last_login_at,
              u.last_login_ip, u.created_at, u.updated_at, u.deleted
            FROM `user` u
            WHERE u.deleted = 0
            <if test='status != null'>AND u.status = #{status}</if>
            <if test='keyword != null'>
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%')
                   OR u.phone LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test='role != null'>
              AND EXISTS (
                SELECT 1
                FROM user_role ur
                INNER JOIN role r ON r.id = ur.role_id
                WHERE ur.user_id = u.id AND r.code = #{role} AND r.enabled = 1
              )
            </if>
            ORDER BY u.created_at DESC, u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<User> findForAdmin(
            @Param("status") UserStatus status,
            @Param("role") RoleCode role,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(*) > 0
            FROM `user`
            WHERE email = #{email} AND deleted = 0
              AND (#{excludedUserId} IS NULL OR id <> #{excludedUserId})
            """)
    /** 执行 existsByEmail 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean existsByEmail(@Param("email") String email, @Param("excludedUserId") Long excludedUserId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM `user`
            WHERE phone = #{phone} AND deleted = 0
              AND (#{excludedUserId} IS NULL OR id <> #{excludedUserId})
            """)
    /** 执行 existsByPhone 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean existsByPhone(@Param("phone") String phone, @Param("excludedUserId") Long excludedUserId);

    @Insert("""
            INSERT INTO `user` (
                username, password_hash, nickname, avatar_url, email, phone, gender, bio, status
            ) VALUES (
                #{username}, #{passwordHash}, #{nickname}, #{avatarUrl}, #{email}, #{phone},
                #{gender}, #{bio}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(User user);

    @Update("""
            UPDATE `user`
            SET nickname = #{nickname},
                avatar_url = #{avatarUrl},
                email = #{email},
                phone = #{phone},
                gender = #{gender},
                bio = #{bio}
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 updateProfile 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateProfile(User user);

    @Update("UPDATE `user` SET avatar_url = NULL WHERE id = #{userId} AND deleted = 0")
    /** 删除、移除或清理头像Url，同时维护关联数据和权限不变量。 */
    int clearAvatarUrl(Long userId);

    @Update("""
            UPDATE `user`
            SET password_hash = #{passwordHash}
            WHERE id = #{userId} AND deleted = 0
            """)
    /** 执行 updatePasswordHash 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE `user`
            SET last_login_at = #{lastLoginAt}, last_login_ip = #{lastLoginIp}
            WHERE id = #{userId} AND deleted = 0
            """)
    /** 执行 updateLastLogin 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateLastLogin(
            @Param("userId") Long userId,
            @Param("lastLoginAt") LocalDateTime lastLoginAt,
            @Param("lastLoginIp") String lastLoginIp
    );

    @Update("""
            UPDATE `user`
            SET status = #{status}
            WHERE id = #{userId} AND deleted = 0
            """)
    /** 执行 updateStatus 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateStatus(@Param("userId") Long userId, @Param("status") UserStatus status);
}
