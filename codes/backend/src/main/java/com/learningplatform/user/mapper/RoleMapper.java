package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.Role;
import com.learningplatform.user.domain.RoleCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface RoleMapper {

    @Select("""
            SELECT id, code, name, description, enabled, created_at, updated_at
            FROM `role`
            WHERE code = #{code} AND enabled = 1
            """)
    Optional<Role> findEnabledByCode(RoleCode code);

    @Select("""
            SELECT r.id, r.code, r.name, r.description, r.enabled, r.created_at, r.updated_at
            FROM `role` r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.enabled = 1
            ORDER BY r.id
            """)
    List<Role> findEnabledByUserId(Long userId);
}
