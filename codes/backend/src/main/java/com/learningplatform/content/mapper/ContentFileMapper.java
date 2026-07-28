/* 文件职责：定义学习资料文件的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.mapper;

import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;
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
 * 定义学习资料文件的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ContentFileMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, content_id, file_role, original_name, object_name, bucket_name, mime_type,
            extension, size_bytes, checksum_sha256, sort_order, duration_seconds, status,
            uploaded_by, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_file WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<ContentFile> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_file
            WHERE content_id = #{contentId} AND deleted = 0 AND status = 'ACTIVE'
            ORDER BY file_role ASC, sort_order ASC, id ASC
            """)
    /** 执行 findByContentId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<ContentFile> findByContentId(Long contentId);

    @Select("""
            SELECT COUNT(*)
            FROM content_file
            WHERE content_id = #{contentId} AND deleted = 0 AND status = 'ACTIVE'
            """)
    /** 执行 countByContentId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int countByContentId(Long contentId);

    @Select("""
            SELECT COUNT(*)
            FROM content_file
            WHERE content_id = #{contentId} AND file_role = #{fileRole}
              AND deleted = 0 AND status = 'ACTIVE'
            """)
    /** 执行 countByContentIdAndRole 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int countByContentIdAndRole(
            @Param("contentId") Long contentId,
            @Param("fileRole") ContentFileRole fileRole
    );

    @Insert("""
            INSERT INTO content_file (
                content_id, file_role, original_name, object_name, bucket_name, mime_type,
                extension, size_bytes, checksum_sha256, sort_order, duration_seconds, status,
                uploaded_by
            ) VALUES (
                #{contentId}, #{fileRole}, #{originalName}, #{objectName}, #{bucketName}, #{mimeType},
                #{extension}, #{sizeBytes}, #{checksumSha256}, #{sortOrder}, #{durationSeconds},
                #{status}, #{uploadedBy}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(ContentFile file);

    @Update("""
            UPDATE content_file
            SET deleted = 1, status = 'DELETED'
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDelete(Long id);
}
