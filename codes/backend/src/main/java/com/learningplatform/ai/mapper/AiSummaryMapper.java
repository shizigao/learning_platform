/* 文件职责：定义AI总结的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiSummary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
/**
 * 定义AI总结的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface AiSummaryMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            s.id, s.task_id, s.content_id, s.summary_text,
            s.knowledge_points_json, s.review_outline, s.source_version,
            s.created_at, s.updated_at
            """;

    @Insert("""
            INSERT INTO ai_summary (
                task_id, content_id, summary_text, knowledge_points_json,
                review_outline, source_version
            ) VALUES (
                #{taskId}, #{contentId}, #{summaryText}, #{knowledgePointsJson},
                #{reviewOutline}, #{sourceVersion}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(AiSummary summary);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_summary s
            WHERE s.task_id = #{taskId}
            """)
    /** 执行 findByTaskId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiSummary> findByTaskId(Long taskId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_summary s
            INNER JOIN ai_task t ON t.id = s.task_id
            WHERE s.content_id = #{contentId}
              AND t.user_id = #{userId}
              AND t.status = 'SUCCEEDED'
            ORDER BY s.created_at DESC, s.id DESC
            LIMIT 1
            """)
    /** 执行 findLatestByContentAndUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiSummary> findLatestByContentAndUser(
            @Param("contentId") Long contentId,
            @Param("userId") Long userId
    );
}
