/* 文件职责：定义考试Statistics的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamQuestionStatistics;
import com.learningplatform.exam.domain.ExamStatisticsSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义考试Statistics的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
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
    /** 执行 summary 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 questionStatistics 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    List<ExamQuestionStatistics> questionStatistics(Long examId);
}
