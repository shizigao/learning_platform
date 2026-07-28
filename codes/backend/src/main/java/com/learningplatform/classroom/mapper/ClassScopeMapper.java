/* 文件职责：定义班级范围的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.mapper;

import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.exam.domain.Exam;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义班级范围的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ClassScopeMapper {
    @Delete("DELETE FROM content_class_scope WHERE content_id = #{contentId}")
    /** 执行 deleteContentScopes 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteContentScopes(Long contentId);

    @Insert("""
            INSERT INTO content_class_scope (content_id, class_id)
            VALUES (#{contentId}, #{classId})
            """)
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertContentScope(@Param("contentId") Long contentId, @Param("classId") Long classId);

    @Select("""
            SELECT class_id
            FROM content_class_scope
            WHERE content_id = #{contentId}
            ORDER BY class_id
            """)
    /** 执行 findContentClassIds 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Long> findContentClassIds(Long contentId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM content_class_scope ccs
            INNER JOIN learning_class lc
              ON lc.id = ccs.class_id AND lc.status = 'ACTIVE' AND lc.deleted = 0
            INNER JOIN class_member cm
              ON cm.class_id = ccs.class_id AND cm.status = 'ACTIVE'
            WHERE ccs.content_id = #{contentId} AND cm.user_id = #{userId}
            """)
    /** 判断是否满足学习资料访问权条件，不修改持久化状态。 */
    boolean hasContentAccess(
            @Param("contentId") Long contentId,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT COUNT(*)
            FROM learning_content lc
            INNER JOIN content_class_scope ccs ON ccs.content_id = lc.id
            WHERE ccs.class_id = #{classId}
              AND lc.distribution_mode = 'CLASS'
              AND lc.status = 'PUBLISHED' AND lc.deleted = 0
            """)
    /** 执行 countPublishedContents 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countPublishedContents(Long classId);

    @Select("""
            SELECT lc.id, lc.publisher_id, publisher.nickname AS publisher_name,
                   lc.category_id, category.name AS category_name,
                   lc.title, lc.summary, lc.content_type, lc.article_body,
                   lc.cover_file_id, lc.distribution_mode, lc.is_free AS free, lc.price,
                   lc.status, lc.rejection_reason, lc.view_count, lc.like_count,
                   lc.favorite_count, lc.comment_count, lc.submitted_at, lc.published_at,
                   lc.created_at, lc.updated_at, lc.deleted
            FROM learning_content lc
            INNER JOIN content_class_scope ccs ON ccs.content_id = lc.id
            INNER JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE ccs.class_id = #{classId}
              AND lc.distribution_mode = 'CLASS'
              AND lc.status = 'PUBLISHED' AND lc.deleted = 0
            ORDER BY lc.published_at DESC, lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    /** 执行 findPublishedContents 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findPublishedContents(
            @Param("classId") Long classId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Delete("DELETE FROM exam_class_scope WHERE exam_id = #{examId}")
    /** 执行 deleteExamScopes 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteExamScopes(Long examId);

    @Insert("""
            INSERT INTO exam_class_scope (exam_id, class_id)
            VALUES (#{examId}, #{classId})
            """)
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertExamScope(@Param("examId") Long examId, @Param("classId") Long classId);

    @Select("""
            SELECT class_id
            FROM exam_class_scope
            WHERE exam_id = #{examId}
            ORDER BY class_id
            """)
    /** 执行 findExamClassIds 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Long> findExamClassIds(Long examId);

    @Select("""
            SELECT COUNT(*) > 0
            FROM exam_class_scope ecs
            INNER JOIN learning_class lc
              ON lc.id = ecs.class_id AND lc.status = 'ACTIVE' AND lc.deleted = 0
            INNER JOIN class_member cm
              ON cm.class_id = ecs.class_id AND cm.status = 'ACTIVE'
            WHERE ecs.exam_id = #{examId} AND cm.user_id = #{userId}
            """)
    /** 判断是否满足考试访问权条件，不修改持久化状态。 */
    boolean hasExamAccess(@Param("examId") Long examId, @Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT cm.user_id
            FROM exam_class_scope ecs
            INNER JOIN class_member cm
              ON cm.class_id = ecs.class_id AND cm.status = 'ACTIVE'
            INNER JOIN learning_class lc
              ON lc.id = ecs.class_id AND lc.status = 'ACTIVE' AND lc.deleted = 0
            WHERE ecs.exam_id = #{examId}
            ORDER BY cm.user_id
            """)
    /** 执行 findActiveMemberIdsForExam 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Long> findActiveMemberIdsForExam(Long examId);

    @Select("""
            SELECT COUNT(*)
            FROM exam e
            INNER JOIN exam_class_scope ecs ON ecs.exam_id = e.id
            WHERE ecs.class_id = #{classId}
              AND e.assignment_mode = 'CLASS'
              AND e.status IN ('PUBLISHED', 'ONGOING', 'FINISHED')
              AND e.deleted = 0
            """)
    /** 执行 countClassExams 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countClassExams(Long classId);

    @Select("""
            SELECT e.id, e.publisher_id, e.paper_id, e.name, e.instructions,
                   e.assignment_mode, e.start_at, e.end_at, e.duration_minutes,
                   e.passing_score, e.show_result_immediately,
                   e.show_answer_after_finish, e.status, e.published_at,
                   e.finished_at, e.version, e.created_at, e.updated_at, e.deleted
            FROM exam e
            INNER JOIN exam_class_scope ecs ON ecs.exam_id = e.id
            WHERE ecs.class_id = #{classId}
              AND e.assignment_mode = 'CLASS'
              AND e.status IN ('PUBLISHED', 'ONGOING', 'FINISHED')
              AND e.deleted = 0
            ORDER BY e.start_at DESC, e.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    /** 执行 findClassExams 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Exam> findClassExams(
            @Param("classId") Long classId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
