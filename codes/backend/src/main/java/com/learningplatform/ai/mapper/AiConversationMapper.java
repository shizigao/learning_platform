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
public interface AiConversationMapper {
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
    int insert(AiConversation conversation);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_conversation
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
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
    List<AiConversation> findByUserAndContent(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId
    );

    @Update("""
            UPDATE ai_conversation
            SET last_message_at = #{lastMessageAt}
            WHERE id = #{id} AND user_id = #{userId} AND deleted = 0
            """)
    int touch(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("lastMessageAt") LocalDateTime lastMessageAt
    );
}
