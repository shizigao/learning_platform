/* 文件职责：定义考试的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义考试的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
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
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<Exam> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam e
            WHERE e.id = #{id} AND e.deleted = 0
            FOR UPDATE
            """)
    /** 执行 findByIdForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
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
    /** 执行 updateDraft 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateDraft(Exam exam);

    @Update("""
            UPDATE exam
            SET status = 'PUBLISHED', published_at = #{publishedAt}, version = version + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'DRAFT'
            """)
    /** 执行 publish 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int publish(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Update("""
            UPDATE exam
            SET status = 'CANCELLED', version = version + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 判断是否满足cel条件，不修改持久化状态。 */
    int cancel(Long id);

    @Update("""
            UPDATE exam
            SET deleted = 1
            WHERE id = #{id} AND publisher_id = #{publisherId}
              AND deleted = 0 AND status = 'DRAFT'
            """)
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
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
    /** 执行 countByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 countForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findAssignedToCandidate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Exam> findAssignedToCandidate(Long userId);
}
