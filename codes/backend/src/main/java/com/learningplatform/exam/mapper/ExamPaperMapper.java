package com.learningplatform.exam.mapper;

import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamPaperStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Mapper
public interface ExamPaperMapper {
    String COLUMNS = """
            p.id, p.creator_id, p.name, p.description, p.total_score,
            p.question_count, p.status, p.created_at, p.updated_at, p.deleted
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM exam_paper p
            WHERE p.id = #{id} AND p.deleted = 0
            """)
    Optional<ExamPaper> findById(Long id);

    @Insert("""
            INSERT INTO exam_paper (
                creator_id, name, description, total_score, question_count, status
            ) VALUES (
                #{creatorId}, #{name}, #{description}, 0.00, 0, 'DRAFT'
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamPaper paper);

    @Update("""
            UPDATE exam_paper
            SET name = #{name}, description = #{description}
            WHERE id = #{id} AND deleted = 0 AND status IN ('DRAFT', 'READY')
            """)
    int update(ExamPaper paper);

    @Update("""
            UPDATE exam_paper
            SET total_score = #{totalScore},
                question_count = #{questionCount},
                status = #{status}
            WHERE id = #{paperId} AND deleted = 0 AND status IN ('DRAFT', 'READY')
            """)
    int updateStatistics(
            @Param("paperId") Long paperId,
            @Param("totalScore") BigDecimal totalScore,
            @Param("questionCount") int questionCount,
            @Param("status") ExamPaperStatus status
    );

    @Update("""
            UPDATE exam_paper
            SET deleted = 1
            WHERE id = #{id} AND deleted = 0 AND status IN ('DRAFT', 'READY')
            """)
    int softDelete(Long id);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM exam_paper p
            WHERE p.creator_id = #{creatorId} AND p.deleted = 0
            <if test='status != null'>AND p.status = #{status}</if>
            <if test='keyword != null'>AND p.name LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    long countByCreator(
            @Param("creatorId") Long creatorId,
            @Param("status") ExamPaperStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM exam_paper p
            WHERE p.creator_id = #{creatorId} AND p.deleted = 0
            <if test='status != null'>AND p.status = #{status}</if>
            <if test='keyword != null'>AND p.name LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY p.updated_at DESC, p.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ExamPaper> findByCreator(
            @Param("creatorId") Long creatorId,
            @Param("status") ExamPaperStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM exam WHERE paper_id = #{paperId} AND deleted = 0")
    long countExamReferences(Long paperId);
}
