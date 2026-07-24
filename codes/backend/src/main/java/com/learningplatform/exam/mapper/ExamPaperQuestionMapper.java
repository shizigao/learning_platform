package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamPaperQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamPaperQuestionMapper {
    @Select("""
            SELECT id, paper_id, question_id, sort_order, score,
                   question_type_snapshot, stem_snapshot, options_snapshot,
                   answer_snapshot, analysis_snapshot, created_at
            FROM exam_paper_question
            WHERE paper_id = #{paperId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<ExamPaperQuestion> findByPaperId(Long paperId);

    @Insert("""
            INSERT INTO exam_paper_question (
                paper_id, question_id, sort_order, score, question_type_snapshot,
                stem_snapshot, options_snapshot, answer_snapshot, analysis_snapshot
            ) VALUES (
                #{paperId}, #{questionId}, #{sortOrder}, #{score}, #{questionTypeSnapshot},
                #{stemSnapshot}, #{optionsSnapshot}, #{answerSnapshot}, #{analysisSnapshot}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamPaperQuestion question);

    @Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId}")
    int deleteByPaperId(Long paperId);
}
