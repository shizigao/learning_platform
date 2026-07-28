/* 文件职责：定义AI消息的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义AI消息的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface AiMessageMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, conversation_id, task_id, role, content, sequence_no,
            token_count, created_at
            """;

    @Insert("""
            INSERT INTO ai_message (
                conversation_id, task_id, role, content, sequence_no, token_count
            ) VALUES (
                #{conversationId}, #{taskId}, #{role}, #{content}, #{sequenceNo}, #{tokenCount}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(AiMessage message);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_message
            WHERE conversation_id = #{conversationId}
            ORDER BY sequence_no ASC
            """)
    /** 执行 findByConversationId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<AiMessage> findByConversationId(Long conversationId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_message
            WHERE task_id = #{taskId} AND role = 'ASSISTANT'
            LIMIT 1
            """)
    /** 执行 findAssistantByTaskId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiMessage> findAssistantByTaskId(Long taskId);

    @Select("""
            SELECT COALESCE(MAX(sequence_no), 0)
            FROM ai_message
            WHERE conversation_id = #{conversationId}
            """)
    /** 执行 maxSequenceNo 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int maxSequenceNo(Long conversationId);
}
