/* 文件职责：定义学习进度的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义学习进度的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface LearningProgressMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
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
    /** 执行 find 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(LearningProgress progress);

    @Update("""
            UPDATE learning_progress
            SET last_learned_at = #{learnedAt},
                progress_percent = #{progressPercent},
                last_position = #{lastPosition},
                completed_at = CASE
                    WHEN #{progressPercent} >= 100 THEN COALESCE(completed_at, #{learnedAt})
                    ELSE NULL
                END
            WHERE user_id = #{userId} AND content_id = #{contentId}
            """)
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId,
            @Param("progressPercent") BigDecimal progressPercent,
            @Param("lastPosition") String lastPosition,
            @Param("learnedAt") LocalDateTime learnedAt
    );
}
