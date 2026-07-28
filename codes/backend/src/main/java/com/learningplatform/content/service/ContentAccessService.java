/* 文件职责：实现学习资料访问权业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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

/**
 * 学习资料资源级访问控制的唯一入口。
 *
 * <p>它组合资料发布状态、管理员/发布者身份、班级成员关系和购买权益。
 * Controller、学习行为、AI 和文件下载均应复用本服务，避免不同入口产生权限差异。</p>
 */
@Service
public class ContentAccessService {
    /** 访问学习资料持久化数据。 */
    private final LearningContentMapper contentMapper;
    /** 访问文件持久化数据。 */
    private final ContentFileMapper fileMapper;
    /** 委托权益执行对应领域规则。 */
    private final EntitlementService entitlementService;
    /** 委托存储执行对应领域规则。 */
    private final MinioStorageService storageService;
    /** 访问班级范围持久化数据。 */
    private final ClassScopeMapper classScopeMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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

    /**
     * 判断用户是否可访问资料正文。
     * 未发布资料对所有读者不可见；管理员和发布者只对已发布资料享有直通权限。
     */
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

    /** 校验访问权及相关业务前置条件，不满足时抛出明确业务异常。 */
    public LearningContent requireAccess(Long contentId, Long userId, boolean requesterAdmin) {
        // 尝试获得该资料的内容，点击getPublished
        LearningContent content = getPublished(contentId);
        if (!hasAccess(userId, requesterAdmin, content)) {
            if (content.getDistributionMode() == ContentDistributionMode.CLASS) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "仅资料发放班级的有效成员可以访问");
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "购买该资料后才能访问正文或文件");
        }
        return content;
    }

    /** 校验资料访问权和文件归属后生成短期预览地址。 */
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

    /** 校验资料访问权和文件归属后生成带原文件名的短期下载地址。 */
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

    /**
     * 发放单份资料访问权。
     *
     * @param sourceOrderItemId 来源订单项；非空时由权益层保证同一订单项只发放一次
     * @param expiresAt 访问权到期时间；{@code null} 表示长期有效
     */
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

    /** 返回发布。 */
    private LearningContent getPublished(Long contentId) {
        // 调用contentMapper.findById(contentId)，点击findById
        LearningContent content = contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        return content;
    }

    /** 返回学习资料文件。 */
    private ContentFile getContentFile(Long contentId, Long fileId) {
        ContentFile file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在"));
        if (!contentId.equals(file.getContentId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        return file;
    }
}
