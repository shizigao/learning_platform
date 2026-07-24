package com.learningplatform.question.mapper;

import com.learningplatform.question.domain.Question;
import com.learningplatform.question.domain.QuestionType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface QuestionMapper {
    String COLUMNS = """
            q.id, q.bank_id, q.creator_id, q.question_type, q.stem,
            q.answer_json, q.answer_text, q.analysis, q.default_score,
            q.fill_blank_auto_gradable, q.case_sensitive, q.status,
            q.created_at, q.updated_at, q.deleted
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM question q
            JOIN question_bank qb ON qb.id = q.bank_id AND qb.deleted = 0
            WHERE q.id = #{id} AND q.deleted = 0
            """)
    Optional<Question> findById(Long id);

    @Insert("""
            INSERT INTO question (
                bank_id, creator_id, question_type, stem, answer_json, answer_text,
                analysis, default_score, fill_blank_auto_gradable, case_sensitive, status
            ) VALUES (
                #{bankId}, #{creatorId}, #{questionType}, #{stem}, #{answerJson}, #{answerText},
                #{analysis}, #{defaultScore}, #{fillBlankAutoGradable}, #{caseSensitive}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question question);

    @Update("""
            UPDATE question
            SET bank_id = #{bankId},
                question_type = #{questionType},
                stem = #{stem},
                answer_json = #{answerJson},
                answer_text = #{answerText},
                analysis = #{analysis},
                default_score = #{defaultScore},
                fill_blank_auto_gradable = #{fillBlankAutoGradable},
                case_sensitive = #{caseSensitive}
            WHERE id = #{id} AND deleted = 0
            """)
    int update(Question question);

    @Update("UPDATE question SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDelete(Long id);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM question q
            JOIN question_bank qb ON qb.id = q.bank_id AND qb.deleted = 0
            WHERE q.deleted = 0 AND qb.owner_id = #{ownerId}
            <if test='bankId != null'>AND q.bank_id = #{bankId}</if>
            <if test='questionType != null'>AND q.question_type = #{questionType}</if>
            <if test='keyword != null'>AND q.stem LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    long countByOwner(
            @Param("ownerId") Long ownerId,
            @Param("bankId") Long bankId,
            @Param("questionType") QuestionType questionType,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM question q
            JOIN question_bank qb ON qb.id = q.bank_id AND qb.deleted = 0
            WHERE q.deleted = 0 AND qb.owner_id = #{ownerId}
            <if test='bankId != null'>AND q.bank_id = #{bankId}</if>
            <if test='questionType != null'>AND q.question_type = #{questionType}</if>
            <if test='keyword != null'>AND q.stem LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY q.updated_at DESC, q.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Question> findByOwner(
            @Param("ownerId") Long ownerId,
            @Param("bankId") Long bankId,
            @Param("questionType") QuestionType questionType,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
