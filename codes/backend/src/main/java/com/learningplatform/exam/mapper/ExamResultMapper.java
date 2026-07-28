/* 文件职责：定义考试成绩的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义考试成绩的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamResultMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
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
    /** 执行 findByAttemptId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findCandidateResult 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findRecentCompletedForWrongReview 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
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
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(ExamResult result);
}
