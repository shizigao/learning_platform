/* 文件职责：实现学习资料文件业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.ContentFileStatus;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.dto.ContentFileResponse;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.storage.MinioStorageService;
import com.learningplatform.content.storage.StorageUploadRequest;
import com.learningplatform.content.storage.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
/**
 * 实现学习资料文件业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ContentFileService {
    private static final Logger log = LoggerFactory.getLogger(ContentFileService.class);

    /** 访问文件持久化数据。 */
    private final ContentFileMapper fileMapper;
    /** 访问学习资料持久化数据。 */
    private final LearningContentMapper contentMapper;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;
    /** 委托存储执行对应领域规则。 */
    private final MinioStorageService storageService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ContentFileService(
            ContentFileMapper fileMapper,
            LearningContentMapper contentMapper,
            LearningContentService contentService,
            MinioStorageService storageService
    ) {
        this.fileMapper = fileMapper;
        this.contentMapper = contentMapper;
        this.contentService = contentService;
        this.storageService = storageService;
    }

    @Transactional
    /** 执行 upload 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentFileResponse upload(
            Long contentId,
            ContentFileRole fileRole,
            MultipartFile multipartFile,
            int sortOrder,
            Integer durationSeconds,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = contentService.getRequired(contentId);
        contentService.assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        contentService.assertEditable(content);
        // 点击store
        StoredObject storedObject = store(
                content,
                fileRole,
                multipartFile,
                requesterUserId,
                requesterAdmin
        );
        try {
            ContentFile file = buildFile(
                    contentId,
                    fileRole,
                    storedObject,
                    sortOrder,
                    durationSeconds,
                    requesterUserId
            );
            if (fileMapper.insert(file) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存文件记录失败");
            }
            if (fileRole == ContentFileRole.COVER
                    && contentMapper.updateCoverFileId(contentId, file.getId()) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新资料封面失败");
            }
            return ContentFileResponse.from(file);
        } catch (RuntimeException exception) {
            compensateStoredObject(storedObject.objectName(), requesterUserId, requesterAdmin);
            throw exception;
        }
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = contentService.getRequired(contentId);
        contentService.assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        contentService.assertEditable(content);
        ContentFile file = getRequired(fileId);
        if (!contentId.equals(file.getContentId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        if (isReferencedByBody(content.getArticleBody(), fileId)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "该文件正在正文中使用，请先删除正文中的图片或文件引用"
            );
        }
        if (fileMapper.softDelete(fileId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        if (content.getCoverFileId() != null && content.getCoverFileId().equals(fileId)) {
            contentMapper.updateCoverFileId(contentId, null);
        }
        storageService.delete(file.getObjectName(), requesterUserId, requesterAdmin);
    }

    /** 执行 previewUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String previewUrl(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        ContentFile file = getOwnedFile(contentId, fileId, requesterUserId, requesterAdmin);
        return storageService.createPreviewUrl(file.getObjectName(), requesterUserId, requesterAdmin);
    }

    /** 执行 downloadUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String downloadUrl(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        // 点击getOwnedFile，从数据库中获得文件的资源路径
        ContentFile file = getOwnedFile(contentId, fileId, requesterUserId, requesterAdmin);
        // 根据文件资源路径，创建minio下载url
        return storageService.createDownloadUrl(
                file.getObjectName(),
                file.getOriginalName(),
                requesterUserId,
                requesterAdmin
        );
    }

    /** 执行 store 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private StoredObject store(
            LearningContent content,
            ContentFileRole fileRole,
            MultipartFile multipartFile,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        try {
            // 点击upload
            return storageService.upload(new StorageUploadRequest(
                    fileRole,
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType(),
                    multipartFile.getSize(),
                    fileMapper.countByContentId(content.getId()),
                    fileMapper.countByContentIdAndRole(content.getId(), fileRole),
                    content.getPublisherId(),
                    requesterUserId,
                    requesterAdmin,
                    multipartFile.getInputStream()
            ));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法读取上传文件");
        }
    }

    /** 执行 buildFile 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ContentFile buildFile(
            Long contentId,
            ContentFileRole fileRole,
            StoredObject storedObject,
            int sortOrder,
            Integer durationSeconds,
            Long requesterUserId
    ) {
        ContentFile file = new ContentFile();
        file.setContentId(contentId);
        file.setFileRole(fileRole);
        file.setOriginalName(storedObject.originalFilename());
        file.setObjectName(storedObject.objectName());
        file.setBucketName(storedObject.bucketName());
        file.setMimeType(storedObject.mimeType());
        file.setExtension(storedObject.extension());
        file.setSizeBytes(storedObject.sizeBytes());
        file.setSortOrder(Math.max(sortOrder, 0));
        file.setDurationSeconds(durationSeconds);
        file.setStatus(ContentFileStatus.ACTIVE);
        file.setUploadedBy(requesterUserId);
        return file;
    }

    /** 返回Owned文件。 */
    private ContentFile getOwnedFile(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = contentService.getRequired(contentId);
        contentService.assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        // 点击getRequired
        ContentFile file = getRequired(fileId);
        if (!contentId.equals(file.getContentId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        return file;
    }

    /** 返回Required。 */
    private ContentFile getRequired(Long fileId) {
        // 点击findById
        return fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在"));
    }

    /** 执行 compensateStoredObject 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void compensateStoredObject(
            String objectName,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        try {
            storageService.delete(objectName, requesterUserId, requesterAdmin);
        } catch (RuntimeException cleanupException) {
            log.error("Unable to remove MinIO object after database failure: {}", objectName, cleanupException);
        }
    }

    /** 判断是否满足ReferencedByBody条件，不修改持久化状态。 */
    private boolean isReferencedByBody(String articleBody, Long fileId) {
        if (articleBody == null || articleBody.isBlank() || fileId == null) {
            return false;
        }
        return articleBody.contains("content-image://" + fileId)
                || articleBody.contains("content-file://" + fileId);
    }
}
