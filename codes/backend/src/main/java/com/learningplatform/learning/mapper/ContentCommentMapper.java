/* 文件职责：定义学习资料评论的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.mapper;

import com.learningplatform.learning.domain.ContentComment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义学习资料评论的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ContentCommentMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, content_id, user_id, parent_id, body, status, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_comment WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ContentComment> findById(Long id);

    @Insert("""
            INSERT INTO content_comment (content_id, user_id, parent_id, body, status)
            VALUES (#{contentId}, #{userId}, #{parentId}, #{body}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ContentComment comment);

    @Select("""
            SELECT COUNT(*)
            FROM content_comment
            WHERE content_id = #{contentId} AND status = 'VISIBLE' AND deleted = 0
            """)
    /** 执行 countVisible 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countVisible(Long contentId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_comment
            WHERE content_id = #{contentId} AND status = 'VISIBLE' AND deleted = 0
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    /** 执行 findVisible 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ContentComment> findVisible(
            @Param("contentId") Long contentId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
