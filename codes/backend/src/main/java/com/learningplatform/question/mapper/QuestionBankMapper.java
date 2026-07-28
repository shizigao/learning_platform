/* 文件职责：定义题目题库的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：题库、题目、选项与标准答案；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义题目题库的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface QuestionBankMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, owner_id, name, description, status, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM question_bank WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<QuestionBank> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM question_bank
            WHERE owner_id = #{ownerId} AND deleted = 0
            ORDER BY updated_at DESC, id DESC
            """)
    /** 执行 findByOwnerId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<QuestionBank> findByOwnerId(Long ownerId);

    @Insert("""
            INSERT INTO question_bank (owner_id, name, description, status)
            VALUES (#{ownerId}, #{name}, #{description}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(QuestionBank bank);

    @Update("""
            UPDATE question_bank
            SET name = #{name}, description = #{description}, status = #{status}
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(QuestionBank bank);

    @Update("UPDATE question_bank SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDelete(Long id);

    @Select("SELECT COUNT(*) FROM question WHERE bank_id = #{bankId} AND deleted = 0")
    /** 执行 countQuestions 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countQuestions(Long bankId);
}
