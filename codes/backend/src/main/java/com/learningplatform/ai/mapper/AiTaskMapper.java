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
public interface AiTaskMapper {
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
    int insert(AiTask task);

    @Select("SELECT " + COLUMNS + " FROM ai_task WHERE id = #{id} AND user_id = #{userId}")
    Optional<AiTask> findByIdAndUser(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Select("SELECT " + COLUMNS + " FROM ai_task WHERE request_id = #{requestId}")
    Optional<AiTask> findByRequestId(String requestId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_task
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<AiTask> findByUserId(Long userId);

    @Update("""
            UPDATE ai_task
            SET status = 'RUNNING', started_at = #{startedAt},
                error_code = NULL, error_message = NULL
            WHERE id = #{id} AND status = 'PENDING'
            """)
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
    int markFailed(
            @Param("id") Long id,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("finishedAt") LocalDateTime finishedAt
    );
}
