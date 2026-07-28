/* 文件职责：定义考试答案的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义考试答案的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamAnswerMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            ea.id, ea.attempt_id, ea.paper_question_id, ea.question_id,
            ea.answer_json, ea.answer_text, ea.max_score, ea.score,
            ea.is_correct AS correct, ea.grading_status, ea.grader_id,
            ea.grader_comment, ea.saved_at, ea.graded_at,
            ea.created_at, ea.updated_at,
            epq.question_type_snapshot AS question_type,
            epq.options_snapshot, epq.answer_snapshot, epq.analysis_snapshot,
            epq.stem_snapshot, epq.sort_order,
            q.fill_blank_auto_gradable, q.case_sensitive
            """;

    @Insert("""
            INSERT IGNORE INTO exam_answer (
                attempt_id, paper_question_id, question_id, max_score,
                grading_status, saved_at
            ) VALUES (
                #{attemptId}, #{paperQuestionId}, #{questionId}, #{maxScore},
                'UNANSWERED', #{savedAt}
            )
            """)
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertIfAbsent(ExamAnswer answer);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_answer ea
            JOIN exam_paper_question epq ON epq.id = ea.paper_question_id
            JOIN question q ON q.id = ea.question_id
            WHERE ea.attempt_id = #{attemptId}
            ORDER BY epq.sort_order ASC, ea.id ASC
            """)
    /** 执行 findByAttemptId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamAnswer> findByAttemptId(Long attemptId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_answer ea
            JOIN exam_paper_question epq ON epq.id = ea.paper_question_id
            JOIN question q ON q.id = ea.question_id
            JOIN exam_attempt a ON a.id = ea.attempt_id
            JOIN exam_result r ON r.attempt_id = a.id
            WHERE a.exam_id = #{examId}
              AND r.grading_completed = TRUE
            ORDER BY epq.sort_order ASC, ea.id ASC
            """)
    /** 执行 findGradedByExamId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamAnswer> findGradedByExamId(Long examId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_answer ea
            JOIN exam_paper_question epq ON epq.id = ea.paper_question_id
            JOIN question q ON q.id = ea.question_id
            WHERE ea.attempt_id = #{attemptId} AND ea.question_id = #{questionId}
            """)
    /** 执行 findOne 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAnswer> findOne(
            @Param("attemptId") Long attemptId,
            @Param("questionId") Long questionId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_answer ea
            JOIN exam_paper_question epq ON epq.id = ea.paper_question_id
            JOIN question q ON q.id = ea.question_id
            WHERE ea.id = #{answerId} AND ea.attempt_id = #{attemptId}
            """)
    /** 执行 findByIdAndAttempt 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamAnswer> findByIdAndAttempt(
            @Param("answerId") Long answerId,
            @Param("attemptId") Long attemptId
    );

    @Update("""
            UPDATE exam_answer
            SET answer_json = #{answerJson},
                answer_text = #{answerText},
                grading_status = #{gradingStatus},
                score = NULL,
                is_correct = NULL,
                saved_at = #{savedAt}
            WHERE id = #{id}
            """)
    /** 执行 updateAnswer 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateAnswer(ExamAnswer answer);

    @Update("""
            UPDATE exam_answer
            SET score = #{score},
                is_correct = #{correct},
                grading_status = #{gradingStatus},
                grader_id = NULL,
                grader_comment = NULL,
                graded_at = #{gradedAt}
            WHERE id = #{id}
            """)
    /** 执行 updateAutomaticGrade 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateAutomaticGrade(ExamAnswer answer);

    @Update("""
            UPDATE exam_answer
            SET score = #{score},
                is_correct = #{correct},
                grading_status = 'GRADED',
                grader_id = #{graderId},
                grader_comment = #{graderComment},
                graded_at = #{gradedAt}
            WHERE id = #{id} AND attempt_id = #{attemptId}
              AND grading_status IN ('PENDING_REVIEW', 'GRADED')
            """)
    /** 执行 updateManualGrade 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateManualGrade(ExamAnswer answer);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
            """)
    /** 执行 countTotal 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int countTotal(Long attemptId);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
              AND grading_status <> 'UNANSWERED'
            """)
    /** 执行 countAnswered 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int countAnswered(Long attemptId);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
              AND grading_status = 'PENDING_REVIEW'
            """)
    /** 执行 countPendingReview 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int countPendingReview(Long attemptId);
}
