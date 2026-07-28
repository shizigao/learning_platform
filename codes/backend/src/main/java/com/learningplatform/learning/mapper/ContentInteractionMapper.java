/* 文件职责：定义学习资料Interaction的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义学习资料Interaction的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ContentInteractionMapper {
    @Select("SELECT COUNT(*) > 0 FROM content_like WHERE user_id = #{userId} AND content_id = #{contentId}")
    /** 判断是否满足Liked条件，不修改持久化状态。 */
    boolean hasLiked(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Insert("INSERT INTO content_like (user_id, content_id) VALUES (#{userId}, #{contentId})")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertLike(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Delete("DELETE FROM content_like WHERE user_id = #{userId} AND content_id = #{contentId}")
    /** 执行 deleteLike 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteLike(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Select("SELECT COUNT(*) > 0 FROM content_favorite WHERE user_id = #{userId} AND content_id = #{contentId}")
    /** 判断是否满足Favorited条件，不修改持久化状态。 */
    boolean hasFavorited(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Insert("INSERT INTO content_favorite (user_id, content_id) VALUES (#{userId}, #{contentId})")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insertFavorite(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Delete("DELETE FROM content_favorite WHERE user_id = #{userId} AND content_id = #{contentId}")
    /** 执行 deleteFavorite 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteFavorite(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Select("""
            SELECT content_id
            FROM content_favorite
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    /** 执行 findFavoriteContentIds 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<Long> findFavoriteContentIds(Long userId);
}
