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
public interface ExamAnswerMapper {
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
    List<ExamAnswer> findGradedByExamId(Long examId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_answer ea
            JOIN exam_paper_question epq ON epq.id = ea.paper_question_id
            JOIN question q ON q.id = ea.question_id
            WHERE ea.attempt_id = #{attemptId} AND ea.question_id = #{questionId}
            """)
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
    int updateManualGrade(ExamAnswer answer);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
            """)
    int countTotal(Long attemptId);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
              AND grading_status <> 'UNANSWERED'
            """)
    int countAnswered(Long attemptId);

    @Select("""
            SELECT COUNT(*)
            FROM exam_answer
            WHERE attempt_id = #{attemptId}
              AND grading_status = 'PENDING_REVIEW'
            """)
    int countPendingReview(Long attemptId);
}
