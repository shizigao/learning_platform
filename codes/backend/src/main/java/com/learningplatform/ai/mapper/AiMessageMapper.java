package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AiMessageMapper {
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
    int insert(AiMessage message);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_message
            WHERE conversation_id = #{conversationId}
            ORDER BY sequence_no ASC
            """)
    List<AiMessage> findByConversationId(Long conversationId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_message
            WHERE task_id = #{taskId} AND role = 'ASSISTANT'
            LIMIT 1
            """)
    Optional<AiMessage> findAssistantByTaskId(Long taskId);

    @Select("""
            SELECT COALESCE(MAX(sequence_no), 0)
            FROM ai_message
            WHERE conversation_id = #{conversationId}
            """)
    int maxSequenceNo(Long conversationId);
}
