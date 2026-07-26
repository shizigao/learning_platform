package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.domain.WrongReviewExam;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;
import java.util.List;

@Mapper
public interface ExamResultMapper {
    String COLUMNS = """
            id, exam_id, attempt_id, user_id, total_score, passing_score,
            passed, correct_count, incorrect_count, unanswered_count,
            grading_completed, visible_to_candidate, generated_at,
            created_at, updated_at
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_result
            WHERE attempt_id = #{attemptId}
            """)
    Optional<ExamResult> findByAttemptId(Long attemptId);

    @Select("""
            SELECT r.id, r.exam_id, r.attempt_id, r.user_id, r.total_score,
                   r.passing_score, r.passed, r.correct_count, r.incorrect_count,
                   r.unanswered_count, r.grading_completed, r.visible_to_candidate,
                   r.generated_at, r.created_at, r.updated_at
            FROM exam_result r
            JOIN exam_attempt a ON a.id = r.attempt_id
            WHERE r.exam_id = #{examId} AND r.user_id = #{userId}
              AND a.attempt_no = 1
            """)
    Optional<ExamResult> findCandidateResult(
            @org.apache.ibatis.annotations.Param("examId") Long examId,
            @org.apache.ibatis.annotations.Param("userId") Long userId
    );

    @Select("""
            SELECT r.id AS result_id, r.exam_id, r.attempt_id,
                   e.name AS exam_name, p.total_score AS full_score,
                   r.total_score, r.passing_score, r.passed, r.generated_at,
                   CASE
                       WHEN e.show_answer_after_finish = TRUE
                        AND e.end_at <= CURRENT_TIMESTAMP
                       THEN TRUE ELSE FALSE
                   END AS answers_visible
            FROM exam_result r
            JOIN exam e ON e.id = r.exam_id
            JOIN exam_paper p ON p.id = e.paper_id
            WHERE r.user_id = #{userId}
              AND r.grading_completed = TRUE
              AND r.visible_to_candidate = TRUE
              AND e.end_at <= CURRENT_TIMESTAMP
            ORDER BY r.generated_at DESC, r.id DESC
            LIMIT 5
            """)
    List<WrongReviewExam> findRecentCompletedForWrongReview(Long userId);

    @Insert("""
            INSERT INTO exam_result (
                exam_id, attempt_id, user_id, total_score, passing_score,
                passed, correct_count, incorrect_count, unanswered_count,
                grading_completed, visible_to_candidate, generated_at
            ) VALUES (
                #{examId}, #{attemptId}, #{userId}, #{totalScore}, #{passingScore},
                #{passed}, #{correctCount}, #{incorrectCount}, #{unansweredCount},
                #{gradingCompleted}, #{visibleToCandidate}, #{generatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamResult result);

    @Update("""
            UPDATE exam_result
            SET total_score = #{totalScore},
                passing_score = #{passingScore},
                passed = #{passed},
                correct_count = #{correctCount},
                incorrect_count = #{incorrectCount},
                unanswered_count = #{unansweredCount},
                grading_completed = #{gradingCompleted},
                visible_to_candidate = #{visibleToCandidate},
                generated_at = #{generatedAt}
            WHERE attempt_id = #{attemptId}
            """)
    int update(ExamResult result);
}
