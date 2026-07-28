/* 文件职责：定义考试作答的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamAttempt;
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
 * 定义考试作答的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamAttemptMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, exam_id, candidate_id, user_id, attempt_no, status,
            started_at, deadline_at, last_saved_at, submitted_at, submission_type,
            objective_score, subjective_score, final_score,
            version, created_at, updated_at
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_attempt
            WHERE exam_id = #{examId} AND user_id = #{userId} AND attempt_no = 1
            """)
    /** 执行 findFirst 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAttempt> findFirst(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_attempt
            WHERE exam_id = #{examId} AND user_id = #{userId} AND attempt_no = 1
            FOR UPDATE
            """)
    /** 执行 findFirstForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAttempt> findFirstForUpdate(
            @Param("examId") Long examId,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_attempt
            WHERE id = #{attemptId}
            FOR UPDATE
            """)
    /** 执行 findByIdForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAttempt> findByIdForUpdate(Long attemptId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_attempt
            WHERE id = #{attemptId}
            """)
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAttempt> findById(Long attemptId);

    @Insert("""
            INSERT INTO exam_attempt (
                exam_id, candidate_id, user_id, attempt_no, status,
                started_at, deadline_at, version
            ) VALUES (
                #{examId}, #{candidateId}, #{userId}, 1, 'IN_PROGRESS',
                #{startedAt}, #{deadlineAt}, 0
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ExamAttempt attempt);

    @Update("""
            UPDATE exam_attempt
            SET last_saved_at = #{savedAt}
            WHERE id = #{attemptId} AND status = 'IN_PROGRESS'
            """)
    /** 转换或规范化uchSaved时间数据，不引入额外持久化副作用。 */
    int touchSavedAt(
            @Param("attemptId") Long attemptId,
            @Param("savedAt") LocalDateTime savedAt
    );

    @Update("""
            UPDATE exam_attempt
            SET status = 'SUBMITTED',
                submitted_at = #{submittedAt},
                submission_type = #{submissionType},
                version = version + 1
            WHERE id = #{attemptId} AND status = 'IN_PROGRESS'
            """)
    /** 执行 markSubmitted 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markSubmitted(
            @Param("attemptId") Long attemptId,
            @Param("submittedAt") LocalDateTime submittedAt,
            @Param("submissionType") com.learningplatform.exam.domain.ExamSubmissionType submissionType
    );

    @Update("""
            UPDATE exam_attempt
            SET status = #{status},
                objective_score = #{objectiveScore},
                subjective_score = #{subjectiveScore},
                final_score = #{finalScore},
                version = version + 1
            WHERE id = #{attemptId}
              AND status IN ('SUBMITTED', 'GRADING', 'COMPLETED')
            """)
    /** 执行 updateGradingState 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateGradingState(
            @Param("attemptId") Long attemptId,
            @Param("status") com.learningplatform.exam.domain.ExamAttemptStatus status,
            @Param("objectiveScore") java.math.BigDecimal objectiveScore,
            @Param("subjectiveScore") java.math.BigDecimal subjectiveScore,
            @Param("finalScore") java.math.BigDecimal finalScore
    );

    @Select("""
            SELECT a.id, a.exam_id, a.candidate_id, a.user_id, a.attempt_no, a.status,
                   a.started_at, a.deadline_at, a.last_saved_at, a.submitted_at,
                   a.submission_type, a.objective_score, a.subjective_score,
                   a.final_score, a.version, a.created_at, a.updated_at,
                   u.username, u.nickname,
                   (SELECT COUNT(*) FROM exam_answer ea
                    WHERE ea.attempt_id = a.id
                      AND ea.grading_status = 'PENDING_REVIEW') AS pending_review_count,
                   COALESCE(r.grading_completed, FALSE) AS grading_completed
            FROM exam_attempt a
            JOIN `user` u ON u.id = a.user_id AND u.deleted = 0
            LEFT JOIN exam_result r ON r.attempt_id = a.id
            WHERE a.exam_id = #{examId}
              AND a.status IN ('SUBMITTED', 'GRADING', 'COMPLETED')
            ORDER BY a.submitted_at DESC, a.id DESC
            """)
    /** 执行 findSubmittedByExam 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamAttempt> findSubmittedByExam(Long examId);

    @Select("""
            SELECT id
            FROM exam_attempt
            WHERE status = 'IN_PROGRESS' AND deadline_at <= #{now}
            ORDER BY deadline_at ASC, id ASC
            LIMIT #{limit}
            """)
    /** 执行 findExpiredIds 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Long> findExpiredIds(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );
}
