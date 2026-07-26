package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamStatus;
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
public interface ExamMapper {
    String COLUMNS = """
            e.id, e.publisher_id, e.paper_id, e.name, e.instructions, e.assignment_mode,
            e.start_at, e.end_at, e.duration_minutes, e.passing_score,
            e.show_result_immediately, e.show_answer_after_finish, e.status,
            e.published_at, e.finished_at, e.version,
            e.created_at, e.updated_at, e.deleted
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE e.id = #{id} AND e.deleted = 0
            """)
    Optional<Exam> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE e.id = #{id} AND e.deleted = 0
            FOR UPDATE
            """)
    Optional<Exam> findByIdForUpdate(Long id);

    @Insert("""
            INSERT INTO exam (
                publisher_id, paper_id, name, instructions, assignment_mode, start_at, end_at,
                duration_minutes, passing_score, show_result_immediately,
                show_answer_after_finish, status, version
            ) VALUES (
                #{publisherId}, #{paperId}, #{name}, #{instructions}, #{assignmentMode},
                #{startAt}, #{endAt},
                #{durationMinutes}, #{passingScore}, #{showResultImmediately},
                #{showAnswerAfterFinish}, 'DRAFT', 0
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Exam exam);

    @Update("""
            UPDATE exam
            SET paper_id = #{paperId},
                name = #{name},
                instructions = #{instructions},
                assignment_mode = #{assignmentMode},
                start_at = #{startAt},
                end_at = #{endAt},
                duration_minutes = #{durationMinutes},
                passing_score = #{passingScore},
                show_result_immediately = #{showResultImmediately},
                show_answer_after_finish = #{showAnswerAfterFinish},
                version = version + 1
            WHERE id = #{id} AND publisher_id = #{publisherId}
              AND deleted = 0 AND status = 'DRAFT'
            """)
    int updateDraft(Exam exam);

    @Update("""
            UPDATE exam
            SET status = 'PUBLISHED', published_at = #{publishedAt}, version = version + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'DRAFT'
            """)
    int publish(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Update("""
            UPDATE exam
            SET status = 'CANCELLED', version = version + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    int cancel(Long id);

    @Update("""
            UPDATE exam
            SET deleted = 1
            WHERE id = #{id} AND publisher_id = #{publisherId}
              AND deleted = 0 AND status = 'DRAFT'
            """)
    int softDelete(@Param("id") Long id, @Param("publisherId") Long publisherId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM exam e
            WHERE e.publisher_id = #{publisherId} AND e.deleted = 0
            <if test='status != null'>AND e.status = #{status}</if>
            <if test='keyword != null'>AND e.name LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    long countByPublisher(
            @Param("publisherId") Long publisherId,
            @Param("status") ExamStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE e.publisher_id = #{publisherId} AND e.deleted = 0
            <if test='status != null'>AND e.status = #{status}</if>
            <if test='keyword != null'>AND e.name LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY e.updated_at DESC, e.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Exam> findByPublisher(
            @Param("publisherId") Long publisherId,
            @Param("status") ExamStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM exam e
            WHERE e.deleted = 0
            <if test='publisherId != null'>AND e.publisher_id = #{publisherId}</if>
            <if test='status != null'>AND e.status = #{status}</if>
            <if test='keyword != null'>AND e.name LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    long countForAdmin(
            @Param("publisherId") Long publisherId,
            @Param("status") ExamStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE e.deleted = 0
            <if test='publisherId != null'>AND e.publisher_id = #{publisherId}</if>
            <if test='status != null'>AND e.status = #{status}</if>
            <if test='keyword != null'>AND e.name LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY e.updated_at DESC, e.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Exam> findForAdmin(
            @Param("publisherId") Long publisherId,
            @Param("status") ExamStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE (
                (e.assignment_mode = 'INDIVIDUAL' AND EXISTS (
                    SELECT 1 FROM exam_candidate ec
                    WHERE ec.exam_id = e.id AND ec.user_id = #{userId}
                ))
                OR
                (e.assignment_mode = 'CLASS' AND EXISTS (
                    SELECT 1
                    FROM exam_class_scope ecs
                    INNER JOIN class_member cm
                      ON cm.class_id = ecs.class_id AND cm.status = 'ACTIVE'
                    INNER JOIN learning_class lc
                      ON lc.id = ecs.class_id AND lc.status = 'ACTIVE' AND lc.deleted = 0
                    WHERE ecs.exam_id = e.id AND cm.user_id = #{userId}
                ))
            )
              AND e.deleted = 0
              AND e.status IN ('PUBLISHED', 'ONGOING', 'FINISHED')
            ORDER BY e.start_at DESC, e.id DESC
            """)
    List<Exam> findAssignedToCandidate(Long userId);
}
