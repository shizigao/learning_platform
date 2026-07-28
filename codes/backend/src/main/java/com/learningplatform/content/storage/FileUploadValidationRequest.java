/* 文件职责：定义文件上传Validation请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：对象存储层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.storage;

import com.learningplatform.content.domain.ContentFileRole;

/**
 * 定义文件上传Validation请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：对象存储保持私有，外部访问只能使用受控的短期签名地址。</p>
 */
public record FileUploadValidationRequest(
        ContentFileRole fileRole,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        int existingFileCount,
        int existingRoleFileCount,
        long resourceOwnerId,
        long requesterUserId,
        boolean requesterAdmin
) {
}
