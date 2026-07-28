/* 文件职责：定义题目选项的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：题库、题目、选项与标准答案；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.mapper;

import com.learningplatform.question.domain.QuestionOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义题目选项的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface QuestionOptionMapper {
    @Select("""
            SELECT id, question_id, option_key, option_text, is_correct AS correct,
                   sort_order, created_at, updated_at
            FROM question_option
            WHERE question_id = #{questionId}
            ORDER BY sort_order ASC, id ASC
            """)
    /** 执行 findByQuestionId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<QuestionOption> findByQuestionId(Long questionId);

    @Insert("""
            INSERT INTO question_option (
                question_id, option_key, option_text, is_correct, sort_order
            ) VALUES (
                #{questionId}, #{optionKey}, #{optionText}, #{correct}, #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(QuestionOption option);

    @Delete("DELETE FROM question_option WHERE question_id = #{questionId}")
    /** 执行 deleteByQuestionId 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteByQuestionId(Long questionId);
}
