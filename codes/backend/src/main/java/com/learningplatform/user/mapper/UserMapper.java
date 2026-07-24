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
public interface UserMapper {
    String USER_COLUMNS = """
            id, username, password_hash, nickname, avatar_url, email, phone, gender, bio,
            status, last_login_at, last_login_ip, created_at, updated_at, deleted
            """;

    @Select("SELECT " + USER_COLUMNS + " FROM `user` WHERE id = #{id} AND deleted = 0")
    Optional<User> findById(Long id);

    @Select("SELECT id FROM `user` WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Optional<Long> lockById(Long id);

    @Select("SELECT " + USER_COLUMNS + " FROM `user` WHERE username = #{username} AND deleted = 0")
    Optional<User> findByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM `user` WHERE username = #{username} AND deleted = 0")
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
    List<User> searchActive(String keyword);

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
    boolean existsByEmail(@Param("email") String email, @Param("excludedUserId") Long excludedUserId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM `user`
            WHERE phone = #{phone} AND deleted = 0
              AND (#{excludedUserId} IS NULL OR id <> #{excludedUserId})
            """)
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
    int updateProfile(User user);

    @Update("""
            UPDATE `user`
            SET password_hash = #{passwordHash}
            WHERE id = #{userId} AND deleted = 0
            """)
    int updatePasswordHash(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("""
            UPDATE `user`
            SET last_login_at = #{lastLoginAt}, last_login_ip = #{lastLoginIp}
            WHERE id = #{userId} AND deleted = 0
            """)
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
    int updateStatus(@Param("userId") Long userId, @Param("status") UserStatus status);
}
