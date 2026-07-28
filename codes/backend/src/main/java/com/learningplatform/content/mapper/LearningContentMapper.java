/* 文件职责：定义学习资料的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.mapper;

import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.LearningContent;
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
 * 定义学习资料的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface LearningContentMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            lc.id, lc.publisher_id, publisher.nickname AS publisher_name,
            lc.category_id, category.name AS category_name,
            lc.title, lc.summary, lc.content_type, lc.article_body,
            lc.cover_file_id, lc.distribution_mode, lc.is_free AS free, lc.price,
            lc.status, lc.rejection_reason,
            lc.view_count, lc.like_count, lc.favorite_count, lc.comment_count,
            lc.submitted_at, lc.published_at, lc.created_at, lc.updated_at, lc.deleted
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.id = #{id} AND lc.deleted = 0
            """)
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<LearningContent> findById(Long id);

    @Insert("""
            INSERT INTO learning_content (
                publisher_id, category_id, title, summary, content_type, article_body,
                distribution_mode, is_free, price, status
            ) VALUES (
                #{publisherId}, #{categoryId}, #{title}, #{summary}, #{contentType}, #{articleBody},
                #{distributionMode}, #{free}, #{price}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(LearningContent content);

    @Update("""
            UPDATE learning_content
            SET category_id = #{categoryId},
                title = #{title},
                summary = #{summary},
                content_type = #{contentType},
                article_body = #{articleBody},
                distribution_mode = #{distributionMode},
                is_free = #{free},
                price = #{price},
                status = #{status},
                rejection_reason = NULL
            WHERE id = #{id} AND publisher_id = #{publisherId} AND deleted = 0
              AND status IN ('DRAFT', 'REJECTED')
            """)
    /** 执行 updateEditable 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateEditable(LearningContent content);

    @Update("""
            UPDATE learning_content
            SET cover_file_id = #{fileId}
            WHERE id = #{contentId} AND deleted = 0
            """)
    /** 执行 updateCoverFileId 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int updateCoverFileId(@Param("contentId") Long contentId, @Param("fileId") Long fileId);

    @Update("""
            UPDATE learning_content
            SET status = 'PENDING_REVIEW', rejection_reason = NULL, submitted_at = #{submittedAt}
            WHERE id = #{id} AND publisher_id = #{publisherId} AND deleted = 0
              AND status IN ('DRAFT', 'REJECTED')
            """)
    /** 执行 submit 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int submit(
            @Param("id") Long id,
            @Param("publisherId") Long publisherId,
            @Param("submittedAt") LocalDateTime submittedAt
    );

    @Update("""
            UPDATE learning_content
            SET status = 'PUBLISHED', rejection_reason = NULL, published_at = #{publishedAt}
            WHERE id = #{id} AND deleted = 0 AND status = 'PENDING_REVIEW'
            """)
    /** 执行 approve 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int approve(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Update("""
            UPDATE learning_content
            SET status = 'REJECTED', rejection_reason = #{reason}
            WHERE id = #{id} AND deleted = 0 AND status = 'PENDING_REVIEW'
            """)
    /** 执行 reject 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int reject(@Param("id") Long id, @Param("reason") String reason);

    @Update("""
            UPDATE learning_content
            SET status = 'OFFLINE'
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 执行 takeOffline 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int takeOffline(Long id);

    @Update("""
            UPDATE learning_content
            SET status = 'PUBLISHED', published_at = #{publishedAt}
            WHERE id = #{id} AND deleted = 0 AND status = 'OFFLINE'
            """)
    /** 执行 republish 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int republish(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    @Update("""
            UPDATE learning_content
            SET deleted = 1
            WHERE id = #{id} AND publisher_id = #{publisherId} AND deleted = 0
              AND status IN ('DRAFT', 'REJECTED')
            """)
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDelete(@Param("id") Long id, @Param("publisherId") Long publisherId);

    @Update("""
            UPDATE learning_content
            SET view_count = view_count + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 执行 incrementViewCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int incrementViewCount(Long id);

    @Update("""
            UPDATE learning_content
            SET like_count = like_count + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 执行 incrementLikeCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int incrementLikeCount(Long id);

    @Update("""
            UPDATE learning_content
            SET like_count = GREATEST(like_count - 1, 0)
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 decrementLikeCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int decrementLikeCount(Long id);

    @Update("""
            UPDATE learning_content
            SET favorite_count = favorite_count + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 执行 incrementFavoriteCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int incrementFavoriteCount(Long id);

    @Update("""
            UPDATE learning_content
            SET favorite_count = GREATEST(favorite_count - 1, 0)
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 decrementFavoriteCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int decrementFavoriteCount(Long id);

    @Update("""
            UPDATE learning_content
            SET comment_count = comment_count + 1
            WHERE id = #{id} AND deleted = 0 AND status = 'PUBLISHED'
            """)
    /** 执行 incrementCommentCount 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int incrementCommentCount(Long id);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM learning_content
            WHERE deleted = 0 AND status = 'PUBLISHED' AND distribution_mode = 'PUBLIC'
            <if test='keyword != null'>AND (title LIKE CONCAT('%', #{keyword}, '%') OR summary LIKE CONCAT('%', #{keyword}, '%'))</if>
            <if test='categoryId != null'>AND category_id = #{categoryId}</if>
            <if test='contentType != null'>AND content_type = #{contentType}</if>
            <if test='free != null'>AND is_free = #{free}</if>
            </script>
            """)
    /** 执行 countPublished 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countPublished(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("contentType") ContentType contentType,
            @Param("free") Boolean free
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.deleted = 0 AND lc.status = 'PUBLISHED' AND lc.distribution_mode = 'PUBLIC'
            <if test='keyword != null'>AND (lc.title LIKE CONCAT('%', #{keyword}, '%') OR lc.summary LIKE CONCAT('%', #{keyword}, '%'))</if>
            <if test='categoryId != null'>AND lc.category_id = #{categoryId}</if>
            <if test='contentType != null'>AND lc.content_type = #{contentType}</if>
            <if test='free != null'>AND lc.is_free = #{free}</if>
            ORDER BY lc.published_at DESC, lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findPublished 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findPublished(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("contentType") ContentType contentType,
            @Param("free") Boolean free,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(*)
            FROM learning_content
            WHERE publisher_id = #{publisherId}
              AND deleted = 0
              AND status = 'PUBLISHED'
              AND distribution_mode = 'PUBLIC'
            """)
    /** 执行 countPublicByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countPublicByPublisher(Long publisherId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.publisher_id = #{publisherId}
              AND lc.deleted = 0
              AND lc.status = 'PUBLISHED'
              AND lc.distribution_mode = 'PUBLIC'
            ORDER BY lc.published_at DESC, lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    /** 执行 findPublicByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findPublicByPublisher(
            @Param("publisherId") Long publisherId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(*) AS content_count,
                   COALESCE(SUM(view_count), 0) AS view_count,
                   COALESCE(SUM(like_count), 0) AS like_count,
                   COALESCE(SUM(favorite_count), 0) AS favorite_count
            FROM learning_content
            WHERE publisher_id = #{publisherId}
              AND deleted = 0
              AND status = 'PUBLISHED'
              AND distribution_mode = 'PUBLIC'
            """)
    /** 执行 publicationStats 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    com.learningplatform.content.domain.ContentPublicationStats publicationStats(Long publisherId);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            WHERE lc.deleted = 0 AND lc.status = 'PUBLISHED' AND lc.distribution_mode = 'PUBLIC'
            <if test='titleKeyword != null'>
              AND lc.title LIKE CONCAT('%', #{titleKeyword}, '%')
            </if>
            <if test='publisherKeyword != null'>
              AND publisher.nickname LIKE CONCAT('%', #{publisherKeyword}, '%')
            </if>
            <if test='excludeContentId != null'>AND lc.id != #{excludeContentId}</if>
            </script>
            """)
    /** 执行 countReferenceCandidates 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countReferenceCandidates(
            @Param("titleKeyword") String titleKeyword,
            @Param("publisherKeyword") String publisherKeyword,
            @Param("excludeContentId") Long excludeContentId
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.deleted = 0 AND lc.status = 'PUBLISHED' AND lc.distribution_mode = 'PUBLIC'
            <if test='titleKeyword != null'>
              AND lc.title LIKE CONCAT('%', #{titleKeyword}, '%')
            </if>
            <if test='publisherKeyword != null'>
              AND publisher.nickname LIKE CONCAT('%', #{publisherKeyword}, '%')
            </if>
            <if test='excludeContentId != null'>AND lc.id != #{excludeContentId}</if>
            ORDER BY lc.published_at DESC, lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findReferenceCandidates 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findReferenceCandidates(
            @Param("titleKeyword") String titleKeyword,
            @Param("publisherKeyword") String publisherKeyword,
            @Param("excludeContentId") Long excludeContentId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM learning_content
            WHERE deleted = 0 AND publisher_id = #{publisherId}
            <if test='status != null'>AND status = #{status}</if>
            <if test='keyword != null'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    /** 执行 countByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countByPublisher(
            @Param("publisherId") Long publisherId,
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.deleted = 0 AND lc.publisher_id = #{publisherId}
            <if test='status != null'>AND lc.status = #{status}</if>
            <if test='keyword != null'>AND lc.title LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY lc.updated_at DESC, lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findByPublisher 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findByPublisher(
            @Param("publisherId") Long publisherId,
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM learning_content
            WHERE deleted = 0
            <if test='status != null'>AND status = #{status}</if>
            <if test='keyword != null'>AND title LIKE CONCAT('%', #{keyword}, '%')</if>
            </script>
            """)
    /** 执行 countForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countForAdmin(
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM learning_content lc
            JOIN `user` publisher ON publisher.id = lc.publisher_id AND publisher.deleted = 0
            LEFT JOIN content_category category ON category.id = lc.category_id
            WHERE lc.deleted = 0
            <if test='status != null'>AND lc.status = #{status}</if>
            <if test='keyword != null'>AND lc.title LIKE CONCAT('%', #{keyword}, '%')</if>
            ORDER BY
              CASE WHEN lc.status = 'PENDING_REVIEW' THEN 0 ELSE 1 END,
              lc.updated_at DESC,
              lc.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 findForAdmin 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<LearningContent> findForAdmin(
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
