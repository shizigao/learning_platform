/* 文件职责：以不可变记录表示StoredObject数据，并作为模块内部或接口层的数据契约。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：对象存储层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.storage;

/**
 * 以不可变记录表示StoredObject数据，并作为模块内部或接口层的数据契约。
 *
 * <p>职责边界：对象存储保持私有，外部访问只能使用受控的短期签名地址。</p>
 */
public record StoredObject(
        String bucketName,
        String objectName,
        String originalFilename,
        String mimeType,
        String extension,
        long sizeBytes
) {
}
