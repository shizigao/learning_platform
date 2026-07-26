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
public interface ClassScopeMapper {
    @Delete("DELETE FROM content_class_scope WHERE content_id = #{contentId}")
    int deleteContentScopes(Long contentId);

    @Insert("""
            INSERT INTO content_class_scope (content_id, class_id)
            VALUES (#{contentId}, #{classId})
            """)
    int insertContentScope(@Param("contentId") Long contentId, @Param("classId") Long classId);

    @Select("""
            SELECT class_id
            FROM content_class_scope
            WHERE content_id = #{contentId}
            ORDER BY class_id
            """)
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
    List<LearningContent> findPublishedContents(
            @Param("classId") Long classId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Delete("DELETE FROM exam_class_scope WHERE exam_id = #{examId}")
    int deleteExamScopes(Long examId);

    @Insert("""
            INSERT INTO exam_class_scope (exam_id, class_id)
            VALUES (#{examId}, #{classId})
            """)
    int insertExamScope(@Param("examId") Long examId, @Param("classId") Long classId);

    @Select("""
            SELECT class_id
            FROM exam_class_scope
            WHERE exam_id = #{examId}
            ORDER BY class_id
            """)
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
    List<Exam> findClassExams(
            @Param("classId") Long classId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
