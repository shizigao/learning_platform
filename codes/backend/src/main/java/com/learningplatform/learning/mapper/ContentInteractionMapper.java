package com.learningplatform.learning.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ContentInteractionMapper {
    @Select("SELECT COUNT(*) > 0 FROM content_like WHERE user_id = #{userId} AND content_id = #{contentId}")
    boolean hasLiked(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Insert("INSERT INTO content_like (user_id, content_id) VALUES (#{userId}, #{contentId})")
    int insertLike(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Delete("DELETE FROM content_like WHERE user_id = #{userId} AND content_id = #{contentId}")
    int deleteLike(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Select("SELECT COUNT(*) > 0 FROM content_favorite WHERE user_id = #{userId} AND content_id = #{contentId}")
    boolean hasFavorited(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Insert("INSERT INTO content_favorite (user_id, content_id) VALUES (#{userId}, #{contentId})")
    int insertFavorite(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Delete("DELETE FROM content_favorite WHERE user_id = #{userId} AND content_id = #{contentId}")
    int deleteFavorite(@Param("userId") Long userId, @Param("contentId") Long contentId);

    @Select("""
            SELECT content_id
            FROM content_favorite
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Long> findFavoriteContentIds(Long userId);
}
