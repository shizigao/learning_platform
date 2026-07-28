/* 文件职责：定义考试试卷的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义考试试卷的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ExamPaperMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
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
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ExamPaper> findById(Long id);

    @Insert("""
            INSERT INTO exam_paper (
                creator_id, name, description, total_score, question_count, status
            ) VALUES (
                #{creatorId}, #{name}, #{description}, 0.00, 0, 'DRAFT'
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ExamPaper paper);

    @Update("""
            UPDATE exam_paper
            SET name = #{name}, description = #{description}
            WHERE id = #{id} AND deleted = 0 AND status IN ('DRAFT', 'READY')
            """)
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(ExamPaper paper);

    @Update("""
            UPDATE exam_paper
            SET total_score = #{totalScore},
                question_count = #{questionCount},
                status = #{status}
            WHERE id = #{paperId} AND deleted = 0 AND status IN ('DRAFT', 'READY')
            """)
    /** 执行 updateStatistics 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
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
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
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
    /** 执行 countByCreator 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findByCreator 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ExamPaper> findByCreator(
            @Param("creatorId") Long creatorId,
            @Param("status") ExamPaperStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("SELECT COUNT(*) FROM exam WHERE paper_id = #{paperId} AND deleted = 0")
    /** 执行 countExamReferences 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countExamReferences(Long paperId);
}
