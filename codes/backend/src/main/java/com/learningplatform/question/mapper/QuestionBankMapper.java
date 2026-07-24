package com.learningplatform.question.mapper;

import com.learningplatform.question.domain.QuestionBank;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface QuestionBankMapper {
    String COLUMNS = """
            id, owner_id, name, description, status, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM question_bank WHERE id = #{id} AND deleted = 0")
    Optional<QuestionBank> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM question_bank
            WHERE owner_id = #{ownerId} AND deleted = 0
            ORDER BY updated_at DESC, id DESC
            """)
    List<QuestionBank> findByOwnerId(Long ownerId);

    @Insert("""
            INSERT INTO question_bank (owner_id, name, description, status)
            VALUES (#{ownerId}, #{name}, #{description}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionBank bank);

    @Update("""
            UPDATE question_bank
            SET name = #{name}, description = #{description}, status = #{status}
            WHERE id = #{id} AND deleted = 0
            """)
    int update(QuestionBank bank);

    @Update("UPDATE question_bank SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDelete(Long id);

    @Select("SELECT COUNT(*) FROM question WHERE bank_id = #{bankId} AND deleted = 0")
    long countQuestions(Long bankId);
}
