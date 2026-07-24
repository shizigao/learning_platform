package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserRoleMapper {

    @Select("""
            SELECT COUNT(*) > 0
            FROM user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    boolean exists(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Insert("""
            INSERT INTO user_role (user_id, role_id, granted_by)
            VALUES (#{userId}, #{roleId}, #{grantedBy})
            """)
    int insert(UserRole userRole);

    @Delete("""
            DELETE FROM user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    int delete(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
