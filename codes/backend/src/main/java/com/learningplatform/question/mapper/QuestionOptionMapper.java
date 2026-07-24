package com.learningplatform.question.mapper;

import com.learningplatform.question.domain.QuestionOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionOptionMapper {
    @Select("""
            SELECT id, question_id, option_key, option_text, is_correct AS correct,
                   sort_order, created_at, updated_at
            FROM question_option
            WHERE question_id = #{questionId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<QuestionOption> findByQuestionId(Long questionId);

    @Insert("""
            INSERT INTO question_option (
                question_id, option_key, option_text, is_correct, sort_order
            ) VALUES (
                #{questionId}, #{optionKey}, #{optionText}, #{correct}, #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionOption option);

    @Delete("DELETE FROM question_option WHERE question_id = #{questionId}")
    int deleteByQuestionId(Long questionId);
}
