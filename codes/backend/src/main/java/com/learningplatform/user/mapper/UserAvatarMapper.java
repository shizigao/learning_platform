/* 文件职责：定义用户头像的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.mapper;

import com.learningplatform.user.domain.UserAvatar;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
/**
 * 定义用户头像的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface UserAvatarMapper {
    @Select("""
            SELECT user_id, bucket_name, object_name, original_name, content_type,
                   extension, size_bytes, created_at, updated_at
            FROM user_avatar
            WHERE user_id = #{userId}
            """)
    /** 执行 findByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 upsert 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int upsert(UserAvatar avatar);

    @Delete("DELETE FROM user_avatar WHERE user_id = #{userId}")
    /** 执行 deleteByUserId 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int deleteByUserId(Long userId);
}
