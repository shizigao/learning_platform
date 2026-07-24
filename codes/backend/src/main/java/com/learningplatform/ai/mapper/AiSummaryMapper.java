package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiSummary;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface AiSummaryMapper {
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
    int insert(AiSummary summary);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_summary s
            WHERE s.task_id = #{taskId}
            """)
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
    Optional<AiSummary> findLatestByContentAndUser(
            @Param("contentId") Long contentId,
            @Param("userId") Long userId
    );
}
