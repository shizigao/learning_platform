package com.learningplatform.content.mapper;

import com.learningplatform.content.domain.ContentCategory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ContentCategoryMapper {
    String COLUMNS = """
            id, parent_id, name, slug, description, sort_order, enabled,
            created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_category WHERE id = #{id} AND deleted = 0")
    Optional<ContentCategory> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_category
            WHERE enabled = 1 AND deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<ContentCategory> findAllEnabled();

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_category
            WHERE deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<ContentCategory> findAll();

    @Select("""
            SELECT COUNT(*) > 0
            FROM content_category
            WHERE slug = #{slug} AND deleted = 0
              AND (#{excludedId} IS NULL OR id <> #{excludedId})
            """)
    boolean existsBySlug(@Param("slug") String slug, @Param("excludedId") Long excludedId);

    @Insert("""
            INSERT INTO content_category (
                parent_id, name, slug, description, sort_order, enabled
            ) VALUES (
                #{parentId}, #{name}, #{slug}, #{description}, #{sortOrder}, #{enabled}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ContentCategory category);

    @Update("""
            UPDATE content_category
            SET parent_id = #{parentId},
                name = #{name},
                slug = #{slug},
                description = #{description},
                sort_order = #{sortOrder},
                enabled = #{enabled}
            WHERE id = #{id} AND deleted = 0
            """)
    int update(ContentCategory category);

    @Update("UPDATE content_category SET deleted = 1, enabled = 0 WHERE id = #{id} AND deleted = 0")
    int softDelete(Long id);
}
