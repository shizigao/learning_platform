package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.UserAvatar;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserAvatarMapper {
    @Select("""
            SELECT user_id, bucket_name, object_name, original_name, content_type,
                   extension, size_bytes, created_at, updated_at
            FROM user_avatar
            WHERE user_id = #{userId}
            """)
    Optional<UserAvatar> findByUserId(Long userId);

    @Insert("""
            INSERT INTO user_avatar (
                user_id, bucket_name, object_name, original_name, content_type,
                extension, size_bytes
            ) VALUES (
                #{userId}, #{bucketName}, #{objectName}, #{originalName}, #{contentType},
                #{extension}, #{sizeBytes}
            )
            ON DUPLICATE KEY UPDATE
                bucket_name = VALUES(bucket_name),
                object_name = VALUES(object_name),
                original_name = VALUES(original_name),
                content_type = VALUES(content_type),
                extension = VALUES(extension),
                size_bytes = VALUES(size_bytes),
                updated_at = CURRENT_TIMESTAMP(3)
            """)
    int upsert(UserAvatar avatar);

    @Delete("DELETE FROM user_avatar WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);
}
