package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.storage.MinioStorageService;
import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;
import com.learningplatform.order.service.EntitlementService;
import com.learningplatform.classroom.mapper.ClassScopeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ContentAccessService {
    private final LearningContentMapper contentMapper;
    private final ContentFileMapper fileMapper;
    private final EntitlementService entitlementService;
    private final MinioStorageService storageService;
    private final ClassScopeMapper classScopeMapper;

    public ContentAccessService(
            LearningContentMapper contentMapper,
            ContentFileMapper fileMapper,
            EntitlementService entitlementService,
            MinioStorageService storageService,
            ClassScopeMapper classScopeMapper
    ) {
        this.contentMapper = contentMapper;
        this.fileMapper = fileMapper;
        this.entitlementService = entitlementService;
        this.storageService = storageService;
        this.classScopeMapper = classScopeMapper;
    }

    public boolean hasAccess(Long userId, boolean requesterAdmin, LearningContent content) {
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            return false;
        }
        if (requesterAdmin || content.getPublisherId().equals(userId)) {
            return true;
        }
        if (content.getDistributionMode() == ContentDistributionMode.CLASS) {
            return userId != null && classScopeMapper.hasContentAccess(content.getId(), userId);
        }
        if (Boolean.TRUE.equals(content.getFree())) return true;
        return entitlementService.hasActiveContentAccess(userId, content.getId());
    }

    public LearningContent requireAccess(Long contentId, Long userId, boolean requesterAdmin) {
        LearningContent content = getPublished(contentId);
        if (!hasAccess(userId, requesterAdmin, content)) {
            if (content.getDistributionMode() == ContentDistributionMode.CLASS) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅资料发放班级的有效成员可以访问");
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "购买该资料后才能访问正文或文件");
        }
        return content;
    }

    public String previewUrl(
            Long contentId,
            Long fileId,
            Long userId,
            boolean requesterAdmin
    ) {
        requireAccess(contentId, userId, requesterAdmin);
        ContentFile file = getContentFile(contentId, fileId);
        return storageService.createAuthorizedPreviewUrl(file.getObjectName());
    }

    public String downloadUrl(
            Long contentId,
            Long fileId,
            Long userId,
            boolean requesterAdmin
    ) {
        requireAccess(contentId, userId, requesterAdmin);
        ContentFile file = getContentFile(contentId, fileId);
        return storageService.createAuthorizedDownloadUrl(file.getObjectName(), file.getOriginalName());
    }

    @Transactional
    public UserEntitlement grantContentAccess(
            Long userId,
            Long contentId,
            Long sourceOrderItemId,
            LocalDateTime expiresAt
    ) {
        getPublished(contentId);
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(userId);
        entitlement.setEntitlementType(EntitlementType.CONTENT_ACCESS);
        entitlement.setResourceId(contentId);
        entitlement.setSourceOrderItemId(sourceOrderItemId);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setEffectiveAt(LocalDateTime.now());
        entitlement.setExpiresAt(expiresAt);
        entitlement.setVersion(0);
        entitlementService.create(entitlement);
        return entitlement;
    }

    private LearningContent getPublished(Long contentId) {
        LearningContent content = contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        return content;
    }

    private ContentFile getContentFile(Long contentId, Long fileId) {
        ContentFile file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在"));
        if (!contentId.equals(file.getContentId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        return file;
    }
}
