/* 文件职责：表示学习资料文件领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示学习资料文件领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ContentFile extends BaseEntity {
    /** 保存学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long contentId;
    /** 保存文件角色，供该类型的业务逻辑读取或更新。 */
    private ContentFileRole fileRole;
    /** 保存original名称，供该类型的业务逻辑读取或更新。 */
    private String originalName;
    /** 保存object名称，供该类型的业务逻辑读取或更新。 */
    private String objectName;
    /** 保存bucket名称，供该类型的业务逻辑读取或更新。 */
    private String bucketName;
    /** 保存mime类型，供该类型的业务逻辑读取或更新。 */
    private String mimeType;
    /** 保存extension，供该类型的业务逻辑读取或更新。 */
    private String extension;
    /** 保存sizeBytes，供该类型的业务逻辑读取或更新。 */
    private Long sizeBytes;
    /** 保存checksumSha256，供该类型的业务逻辑读取或更新。 */
    private String checksumSha256;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;
    /** 保存durationSeconds，供该类型的业务逻辑读取或更新。 */
    private Integer durationSeconds;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ContentFileStatus status;
    /** 保存uploaded按，供该类型的业务逻辑读取或更新。 */
    private Long uploadedBy;

    /** 返回学习资料ID。 */
    public Long getContentId() {
        return contentId;
    }

    /** 更新学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    /** 返回文件角色。 */
    public ContentFileRole getFileRole() {
        return fileRole;
    }

    /** 更新文件角色；调用方仍需遵守所属领域的校验规则。 */
    public void setFileRole(ContentFileRole fileRole) {
        this.fileRole = fileRole;
    }

    /** 返回Original名称。 */
    public String getOriginalName() {
        return originalName;
    }

    /** 更新Original名称；调用方仍需遵守所属领域的校验规则。 */
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    /** 返回Object名称。 */
    public String getObjectName() {
        return objectName;
    }

    /** 更新Object名称；调用方仍需遵守所属领域的校验规则。 */
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    /** 返回Bucket名称。 */
    public String getBucketName() {
        return bucketName;
    }

    /** 更新Bucket名称；调用方仍需遵守所属领域的校验规则。 */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /** 返回Mime类型。 */
    public String getMimeType() {
        return mimeType;
    }

    /** 更新Mime类型；调用方仍需遵守所属领域的校验规则。 */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /** 返回Extension。 */
    public String getExtension() {
        return extension;
    }

    /** 更新Extension；调用方仍需遵守所属领域的校验规则。 */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /** 返回SizeBytes。 */
    public Long getSizeBytes() {
        return sizeBytes;
    }

    /** 更新SizeBytes；调用方仍需遵守所属领域的校验规则。 */
    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    /** 返回ChecksumSha256。 */
    public String getChecksumSha256() {
        return checksumSha256;
    }

    /** 更新ChecksumSha256；调用方仍需遵守所属领域的校验规则。 */
    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 返回DurationSeconds。 */
    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    /** 更新DurationSeconds；调用方仍需遵守所属领域的校验规则。 */
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /** 返回状态。 */
    public ContentFileStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ContentFileStatus status) {
        this.status = status;
    }

    /** 返回Uploaded按。 */
    public Long getUploadedBy() {
        return uploadedBy;
    }

    /** 更新Uploaded按；调用方仍需遵守所属领域的校验规则。 */
    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
