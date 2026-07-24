package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamQuestionStatistics;
import com.learningplatform.exam.domain.ExamStatisticsSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamStatisticsMapper {
    @Select("""
            SELECT COUNT(DISTINCT ec.id) AS total_candidates,
                   COUNT(DISTINCT a.id) AS participated_count,
                   COUNT(DISTINCT CASE WHEN a.submitted_at IS NOT NULL THEN a.id END)
                       AS submitted_count,
                   COUNT(DISTINCT ec.id) - COUNT(DISTINCT a.id)
                       AS not_participated_count,
                   COUNT(DISTINCT CASE WHEN r.grading_completed = TRUE THEN r.id END)
                       AS graded_count,
                   AVG(CASE WHEN r.grading_completed = TRUE THEN r.total_score END)
                       AS average_score,
                   MAX(CASE WHEN r.grading_completed = TRUE THEN r.total_score END)
                       AS highest_score,
                   MIN(CASE WHEN r.grading_completed = TRUE THEN r.total_score END)
                       AS lowest_score,
                   COUNT(DISTINCT CASE
                       WHEN r.grading_completed = TRUE AND r.passed = TRUE THEN r.id
                   END) AS passed_count
            FROM exam_candidate ec
            LEFT JOIN exam_attempt a
              ON a.exam_id = ec.exam_id AND a.candidate_id = ec.id
            LEFT JOIN exam_result r ON r.attempt_id = a.id
            WHERE ec.exam_id = #{examId}
            """)
    ExamStatisticsSummary summary(Long examId);

    @Select("""
            SELECT pq.question_id, pq.sort_order,
                   pq.question_type_snapshot AS question_type,
                   pq.stem_snapshot AS stem, pq.score AS max_score,
                   COUNT(DISTINCT r.id) AS graded_count,
                   COUNT(DISTINCT CASE
                       WHEN r.id IS NOT NULL AND aa.grading_status <> 'UNANSWERED'
                       THEN a.id
                   END) AS answered_count,
                   COUNT(DISTINCT CASE
                       WHEN r.id IS NOT NULL AND aa.is_correct = TRUE
                       THEN a.id
                   END) AS correct_count
            FROM exam e
            JOIN exam_paper_question pq ON pq.paper_id = e.paper_id
            LEFT JOIN exam_attempt a ON a.exam_id = e.id
            LEFT JOIN exam_result r
              ON r.attempt_id = a.id AND r.grading_completed = TRUE
            LEFT JOIN exam_answer aa
              ON aa.attempt_id = a.id AND aa.paper_question_id = pq.id
            WHERE e.id = #{examId} AND e.deleted = FALSE
            GROUP BY pq.id, pq.question_id, pq.sort_order,
                     pq.question_type_snapshot, pq.stem_snapshot, pq.score
            ORDER BY pq.sort_order ASC, pq.id ASC
            """)
    List<ExamQuestionStatistics> questionStatistics(Long examId);
}
