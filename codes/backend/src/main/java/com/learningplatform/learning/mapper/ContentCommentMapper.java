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
public interface ContentCommentMapper {
    String COLUMNS = """
            id, content_id, user_id, parent_id, body, status, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_comment WHERE id = #{id} AND deleted = 0")
    Optional<ContentComment> findById(Long id);

    @Insert("""
            INSERT INTO content_comment (content_id, user_id, parent_id, body, status)
            VALUES (#{contentId}, #{userId}, #{parentId}, #{body}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentComment comment);

    @Select("""
            SELECT COUNT(*)
            FROM content_comment
            WHERE content_id = #{contentId} AND status = 'VISIBLE' AND deleted = 0
            """)
    long countVisible(Long contentId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_comment
            WHERE content_id = #{contentId} AND status = 'VISIBLE' AND deleted = 0
            ORDER BY created_at ASC, id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ContentComment> findVisible(
            @Param("contentId") Long contentId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
