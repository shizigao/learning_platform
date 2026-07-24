package com.learningplatform.learning.mapper;

import com.learningplatform.learning.domain.LearningProgress;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface LearningProgressMapper {
    String COLUMNS = """
            id, user_id, content_id, started_at, last_learned_at, progress_percent,
            last_position, completed_at, created_at, updated_at
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM learning_progress
            WHERE user_id = #{userId} AND content_id = #{contentId}
            """)
    Optional<LearningProgress> find(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM learning_progress
            WHERE user_id = #{userId}
            ORDER BY last_learned_at DESC, id DESC
            """)
    List<LearningProgress> findByUserId(Long userId);

    @Insert("""
            INSERT INTO learning_progress (
                user_id, content_id, started_at, last_learned_at,
                progress_percent, last_position, completed_at
            ) VALUES (
                #{userId}, #{contentId}, #{startedAt}, #{lastLearnedAt},
                #{progressPercent}, #{lastPosition}, #{completedAt}
            )
            """)
    int insert(LearningProgress progress);

    @Update("""
            UPDATE learning_progress
            SET last_learned_at = #{learnedAt},
                progress_percent = GREATEST(progress_percent, #{progressPercent}),
                last_position = #{lastPosition},
                completed_at = CASE
                    WHEN #{progressPercent} >= 100 THEN COALESCE(completed_at, #{learnedAt})
                    ELSE completed_at
                END
            WHERE user_id = #{userId} AND content_id = #{contentId}
            """)
    int update(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId,
            @Param("progressPercent") BigDecimal progressPercent,
            @Param("lastPosition") String lastPosition,
            @Param("learnedAt") LocalDateTime learnedAt
    );
}
