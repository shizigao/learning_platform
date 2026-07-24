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
public interface ContentFileMapper {
    String COLUMNS = """
            id, content_id, file_role, original_name, object_name, bucket_name, mime_type,
            extension, size_bytes, checksum_sha256, sort_order, duration_seconds, status,
            uploaded_by, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM content_file WHERE id = #{id} AND deleted = 0")
    Optional<ContentFile> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM content_file
            WHERE content_id = #{contentId} AND deleted = 0 AND status = 'ACTIVE'
            ORDER BY file_role ASC, sort_order ASC, id ASC
            """)
    List<ContentFile> findByContentId(Long contentId);

    @Select("""
            SELECT COUNT(*)
            FROM content_file
            WHERE content_id = #{contentId} AND deleted = 0 AND status = 'ACTIVE'
            """)
    int countByContentId(Long contentId);

    @Select("""
            SELECT COUNT(*)
            FROM content_file
            WHERE content_id = #{contentId} AND file_role = #{fileRole}
              AND deleted = 0 AND status = 'ACTIVE'
            """)
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
    int insert(ContentFile file);

    @Update("""
            UPDATE content_file
            SET deleted = 1, status = 'DELETED'
            WHERE id = #{id} AND deleted = 0
            """)
    int softDelete(Long id);
}
