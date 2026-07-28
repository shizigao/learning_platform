/* 文件职责：定义学习资料分类的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义学习资料分类的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ContentCategoryMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, parent_id, name, slug, description, sort_order, enabled,
            created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_category WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ContentCategory> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_category
            WHERE enabled = 1 AND deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    /** 执行 findAllEnabled 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ContentCategory> findAllEnabled();

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_category
            WHERE deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    /** 执行 findAll 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ContentCategory> findAll();

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM content_category
            WHERE enabled = 1 AND deleted = 0
            <if test='keyword != null'>
              AND (
                name LIKE CONCAT('%', #{keyword}, '%')
                OR slug LIKE CONCAT('%', #{keyword}, '%')
                OR description LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            </script>
            """)
    /** 执行 countEnabled 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countEnabled(@Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM content_category
            WHERE enabled = 1 AND deleted = 0
            <if test='keyword != null'>
              AND (
                name LIKE CONCAT('%', #{keyword}, '%')
                OR slug LIKE CONCAT('%', #{keyword}, '%')
                OR description LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY sort_order ASC, id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    /** 执行 searchEnabled 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ContentCategory> searchEnabled(
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            SELECT COUNT(*) > 0
            FROM content_category
            WHERE slug = #{slug} AND deleted = 0
              AND (#{excludedId} IS NULL OR id <> #{excludedId})
            """)
    /** 执行 existsBySlug 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean existsBySlug(@Param("slug") String slug, @Param("excludedId") Long excludedId);

    @Insert("""
            INSERT INTO content_category (
                parent_id, name, slug, description, sort_order, enabled
            ) VALUES (
                #{parentId}, #{name}, #{slug}, #{description}, #{sortOrder}, #{enabled}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
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
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(ContentCategory category);

    @Update("UPDATE content_category SET deleted = 1, enabled = 0 WHERE id = #{id} AND deleted = 0")
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDelete(Long id);
}
