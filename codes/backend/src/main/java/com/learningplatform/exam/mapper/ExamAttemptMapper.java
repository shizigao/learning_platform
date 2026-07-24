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
public interface ExamAttemptMapper {
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
    Optional<ExamAttempt> findByIdForUpdate(Long attemptId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_attempt
            WHERE id = #{attemptId}
            """)
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
    int insert(ExamAttempt attempt);

    @Update("""
            UPDATE exam_attempt
            SET last_saved_at = #{savedAt}
            WHERE id = #{attemptId} AND status = 'IN_PROGRESS'
            """)
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
    List<ExamAttempt> findSubmittedByExam(Long examId);

    @Select("""
            SELECT id
            FROM exam_attempt
            WHERE status = 'IN_PROGRESS' AND deadline_at <= #{now}
            ORDER BY deadline_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<Long> findExpiredIds(
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );
}
