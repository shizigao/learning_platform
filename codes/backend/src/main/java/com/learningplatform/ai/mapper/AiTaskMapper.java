/* 文件职责：定义AI任务的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Mapper
/**
 * 定义AI任务的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface AiTaskMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, request_id, user_id, content_id, conversation_id, task_type,
            provider, model, status, input_chars, quota_cost, error_code,
            error_message, started_at, finished_at, created_at, updated_at
            """;

    @Insert("""
            INSERT INTO ai_task (
                request_id, user_id, content_id, conversation_id, task_type,
                provider, model, status, input_chars, quota_cost
            ) VALUES (
                #{requestId}, #{userId}, #{contentId}, #{conversationId}, #{taskType},
                #{provider}, #{model}, #{status}, #{inputChars}, #{quotaCost}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(AiTask task);

    @Select("SELECT " + COLUMNS + " FROM ai_task WHERE id = #{id} AND user_id = #{userId}")
    /** 执行 findByIdAndUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiTask> findByIdAndUser(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Select("SELECT " + COLUMNS + " FROM ai_task WHERE request_id = #{requestId}")
    /** 执行 findByRequestId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiTask> findByRequestId(String requestId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_task
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    /** 执行 findByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<AiTask> findByUserId(Long userId);

    @Update("""
            UPDATE ai_task
            SET status = 'RUNNING', started_at = #{startedAt},
                error_code = NULL, error_message = NULL
            WHERE id = #{id} AND status = 'PENDING'
            """)
    /** 执行 markRunning 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markRunning(
            @Param("id") Long id,
            @Param("startedAt") LocalDateTime startedAt
    );

    @Update("""
            UPDATE ai_task
            SET status = 'SUCCEEDED', finished_at = #{finishedAt},
                error_code = NULL, error_message = NULL
            WHERE id = #{id} AND status = 'RUNNING'
            """)
    /** 执行 markSucceeded 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markSucceeded(
            @Param("id") Long id,
            @Param("finishedAt") LocalDateTime finishedAt
    );

    @Update("""
            UPDATE ai_task
            SET status = 'FAILED', finished_at = #{finishedAt},
                error_code = #{errorCode}, error_message = #{errorMessage}
            WHERE id = #{id} AND status IN ('PENDING', 'RUNNING')
            """)
    /** 执行 markFailed 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markFailed(
            @Param("id") Long id,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("finishedAt") LocalDateTime finishedAt
    );
}
