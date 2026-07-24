package com.learningplatform.content.domain;

import com.learningplatform.common.model.BaseEntity;

public class ContentFile extends BaseEntity {
    private Long contentId;
    private ContentFileRole fileRole;
    private String originalName;
    private String objectName;
    private String bucketName;
    private String mimeType;
    private String extension;
    private Long sizeBytes;
    private String checksumSha256;
    private Integer sortOrder;
    private Integer durationSeconds;
    private ContentFileStatus status;
    private Long uploadedBy;

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public ContentFileRole getFileRole() {
        return fileRole;
    }

    public void setFileRole(ContentFileRole fileRole) {
        this.fileRole = fileRole;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public ContentFileStatus getStatus() {
        return status;
    }

    public void setStatus(ContentFileStatus status) {
        this.status = status;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
