/* 文件职责：定义AI会话的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiConversation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义AI会话的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface AiConversationMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, user_id, content_id, title, status, last_message_at,
            created_at, updated_at, deleted
            """;

    @Insert("""
            INSERT INTO ai_conversation (
                user_id, content_id, title, status
            ) VALUES (
                #{userId}, #{contentId}, #{title}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(AiConversation conversation);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_conversation
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    /** 执行 findByIdAndUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiConversation> findByIdAndUser(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_conversation
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            FOR UPDATE
            """)
    /** 执行 findByIdAndUserForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiConversation> findByIdAndUserForUpdate(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_conversation
            WHERE user_id = #{userId} AND content_id = #{contentId}
              AND deleted = 0
            ORDER BY COALESCE(last_message_at, created_at) DESC, id DESC
            """)
    /** 执行 findByUserAndContent 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<AiConversation> findByUserAndContent(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId
    );

    @Update("""
            UPDATE ai_conversation
            SET last_message_at = #{lastMessageAt}
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    /** 转换或规范化uch数据，不引入额外持久化副作用。 */
    int touch(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("lastMessageAt") LocalDateTime lastMessageAt
    );
}
