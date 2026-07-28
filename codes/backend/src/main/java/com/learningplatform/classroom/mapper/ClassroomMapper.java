/* 文件职责：定义班级的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.mapper;

import com.learningplatform.classroom.domain.ClassAnnouncement;
import com.learningplatform.classroom.domain.ClassMember;
import com.learningplatform.classroom.domain.ClassMemberStatus;
import com.learningplatform.classroom.domain.ClassMemberView;
import com.learningplatform.classroom.domain.ClassRole;
import com.learningplatform.classroom.domain.LearningClass;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义班级的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ClassroomMapper {
    /** 定义 CLASS_COLUMNS 常量，统一该组件使用的固定规则或默认值。 */
    String CLASS_COLUMNS = """
            id, owner_id, name, description, invite_code, invite_enabled,
            status, version, created_at, updated_at, deleted
            """;

    @Select("SELECT " + CLASS_COLUMNS + " FROM learning_class WHERE id = #{id} AND deleted = 0")
    /** 执行 findClassById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<LearningClass> findClassById(Long id);

    @Select("""
            SELECT
            """ + CLASS_COLUMNS + """
            FROM learning_class
            WHERE invite_code = #{inviteCode} AND deleted = 0
            """)
    /** 执行 findClassByInviteCode 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<LearningClass> findClassByInviteCode(String inviteCode);

    @Select("SELECT COUNT(*) > 0 FROM learning_class WHERE invite_code = #{inviteCode}")
    /** 执行 inviteCodeExists 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    boolean inviteCodeExists(String inviteCode);

    @Select("""
            SELECT lc.id, lc.owner_id, lc.name, lc.description, lc.invite_code,
                   lc.invite_enabled, lc.status, lc.version, lc.created_at,
                   lc.updated_at, lc.deleted
            FROM learning_class lc
            INNER JOIN class_member cm ON cm.class_id = lc.id
            WHERE cm.user_id = #{userId} AND cm.status = 'ACTIVE'
              AND cm.role IN ('OWNER', 'ADMIN')
              AND lc.status = 'ACTIVE' AND lc.deleted = 0
            ORDER BY lc.updated_at DESC, lc.id DESC
            """)
    /** 执行 findManagedClasses 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningClass> findManagedClasses(Long userId);

    @Select("""
            SELECT lc.id, lc.owner_id, lc.name, lc.description, lc.invite_code,
                   lc.invite_enabled, lc.status, lc.version, lc.created_at,
                   lc.updated_at, lc.deleted
            FROM learning_class lc
            INNER JOIN class_member cm ON cm.class_id = lc.id
            WHERE cm.user_id = #{userId} AND cm.status = 'ACTIVE'
              AND lc.status = 'ACTIVE' AND lc.deleted = 0
            ORDER BY cm.joined_at DESC, lc.id DESC
            """)
    /** 执行 findJoinedClasses 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningClass> findJoinedClasses(Long userId);

    @Insert("""
            INSERT INTO learning_class (
                owner_id, name, description, invite_code, invite_enabled, status, version
            ) VALUES (
                #{ownerId}, #{name}, #{description}, #{inviteCode}, #{inviteEnabled}, #{status}, 0
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertClass(LearningClass classroom);

    @Update("""
            UPDATE learning_class
            SET name = #{name}, description = #{description}, version = version + 1
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    /** 执行 updateClass 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateClass(LearningClass classroom);

    @Update("""
            UPDATE learning_class
            SET invite_code = #{inviteCode}, version = version + 1
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    /** 执行 updateInviteCode 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateInviteCode(@Param("id") Long id, @Param("inviteCode") String inviteCode);

    @Update("""
            UPDATE learning_class
            SET invite_enabled = #{enabled}, version = version + 1
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    /** 执行 updateInviteEnabled 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateInviteEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);

    @Update("""
            UPDATE learning_class
            SET owner_id = #{ownerId}, version = version + 1
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    /** 执行 updateOwner 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Update("""
            UPDATE learning_class
            SET status = 'ARCHIVED', deleted = 1, invite_enabled = 0, version = version + 1
            WHERE id = #{id} AND status = 'ACTIVE' AND deleted = 0
            """)
    /** 执行 archiveClass 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int archiveClass(Long id);

    @Select("""
            SELECT id, class_id, user_id, role, status, joined_at, left_at, created_at, updated_at
            FROM class_member
            WHERE class_id = #{classId} AND user_id = #{userId}
            """)
    /** 执行 findMember 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ClassMember> findMember(
            @Param("classId") Long classId,
            @Param("userId") Long userId
    );

    @Insert("""
            INSERT INTO class_member (
                class_id, user_id, role, status, joined_at
            ) VALUES (
                #{classId}, #{userId}, #{role}, #{status}, CURRENT_TIMESTAMP(3)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertMember(ClassMember member);

    @Update("""
            UPDATE class_member
            SET role = 'MEMBER', status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP(3), left_at = NULL
            WHERE class_id = #{classId} AND user_id = #{userId} AND status = 'LEFT'
            """)
    /** 执行 rejoinMember 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int rejoinMember(@Param("classId") Long classId, @Param("userId") Long userId);

    @Update("""
            UPDATE class_member
            SET status = #{status}, left_at = CURRENT_TIMESTAMP(3)
            WHERE class_id = #{classId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    /** 执行 updateMemberStatus 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateMemberStatus(
            @Param("classId") Long classId,
            @Param("userId") Long userId,
            @Param("status") ClassMemberStatus status
    );

    @Update("""
            UPDATE class_member
            SET role = #{role}
            WHERE class_id = #{classId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    /** 执行 updateMemberRole 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateMemberRole(
            @Param("classId") Long classId,
            @Param("userId") Long userId,
            @Param("role") ClassRole role
    );

    @Update("""
            UPDATE class_member
            SET role = 'MEMBER', status = 'ACTIVE', joined_at = CURRENT_TIMESTAMP(3), left_at = NULL
            WHERE class_id = #{classId} AND user_id = #{userId} AND status = 'REMOVED'
            """)
    /** 更新成员，通过返回值或版本条件识别并发状态变化。 */
    int restoreMember(@Param("classId") Long classId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM class_member WHERE class_id = #{classId} AND status = 'ACTIVE'")
    /** 执行 countActiveMembers 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countActiveMembers(Long classId);

    @Select("""
            <script>
            SELECT cm.id, cm.class_id, cm.user_id, cm.role, cm.status, cm.joined_at,
                   cm.left_at, cm.created_at, cm.updated_at,
                   u.username, u.nickname
            FROM class_member cm
            INNER JOIN `user` u ON u.id = cm.user_id
            WHERE cm.class_id = #{classId} AND cm.status = 'ACTIVE' AND u.deleted = 0
            <if test='keyword != null'>
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY CASE cm.role
                         WHEN 'OWNER' THEN 0
                         WHEN 'ADMIN' THEN 1
                         ELSE 2
                     END,
                     cm.joined_at ASC, cm.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findActiveMembers 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ClassMemberView> findActiveMembers(
            @Param("classId") Long classId,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM class_member cm
            INNER JOIN `user` u ON u.id = cm.user_id
            WHERE cm.class_id = #{classId} AND cm.status = 'ACTIVE' AND u.deleted = 0
            <if test='keyword != null'>
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    /** 执行 countActiveMembersByKeyword 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countActiveMembersByKeyword(
            @Param("classId") Long classId,
            @Param("keyword") String keyword
    );

    @Select("""
            SELECT COUNT(*)
            FROM exam_class_scope ecs
            INNER JOIN exam e ON e.id = ecs.exam_id
            WHERE ecs.class_id = #{classId}
              AND e.deleted = 0
              AND e.status IN ('PUBLISHED', 'ONGOING')
              AND e.end_at > CURRENT_TIMESTAMP(3)
            """)
    /** 执行 countActiveClassExams 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countActiveClassExams(Long classId);

    @Insert("""
            INSERT INTO class_announcement (class_id, author_id, title, body, pinned)
            VALUES (#{classId}, #{authorId}, #{title}, #{body}, #{pinned})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertAnnouncement(ClassAnnouncement announcement);

    @Select("""
            SELECT ca.id, ca.class_id, ca.author_id, u.nickname AS author_name,
                   ca.title, ca.body, ca.pinned, ca.created_at, ca.updated_at, ca.deleted
            FROM class_announcement ca
            INNER JOIN `user` u ON u.id = ca.author_id
            WHERE ca.id = #{id} AND ca.deleted = 0
            """)
    /** 执行 findAnnouncementById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ClassAnnouncement> findAnnouncementById(Long id);

    @Select("""
            SELECT ca.id, ca.class_id, ca.author_id, u.nickname AS author_name,
                   ca.title, ca.body, ca.pinned, ca.created_at, ca.updated_at, ca.deleted
            FROM class_announcement ca
            INNER JOIN `user` u ON u.id = ca.author_id
            WHERE ca.class_id = #{classId} AND ca.deleted = 0
            ORDER BY ca.pinned DESC, ca.created_at DESC, ca.id DESC
            """)
    /** 执行 findAnnouncements 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ClassAnnouncement> findAnnouncements(Long classId);

    @Update("""
            UPDATE class_announcement
            SET title = #{title}, body = #{body}, pinned = #{pinned}
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 updateAnnouncement 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateAnnouncement(ClassAnnouncement announcement);

    @Update("UPDATE class_announcement SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    /** 执行 softDeleteAnnouncement 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDeleteAnnouncement(Long id);
}
