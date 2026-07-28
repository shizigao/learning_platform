/* 文件职责：定义公告响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassAnnouncement;

import java.time.LocalDateTime;

/**
 * 定义公告响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AnnouncementResponse(
        Long id,
        Long classId,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String title,
        String body,
        Boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static AnnouncementResponse from(
            ClassAnnouncement announcement,
            String authorAvatarUrl
    ) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getClassId(),
                announcement.getAuthorId(),
                announcement.getAuthorName(),
                authorAvatarUrl,
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
