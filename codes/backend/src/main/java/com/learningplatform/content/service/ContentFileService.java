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
public class ContentFileService {
    private static final Logger log = LoggerFactory.getLogger(ContentFileService.class);

    private final ContentFileMapper fileMapper;
    private final LearningContentMapper contentMapper;
    private final LearningContentService contentService;
    private final MinioStorageService storageService;

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

    public String previewUrl(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        ContentFile file = getOwnedFile(contentId, fileId, requesterUserId, requesterAdmin);
        return storageService.createPreviewUrl(file.getObjectName(), requesterUserId, requesterAdmin);
    }

    public String downloadUrl(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        ContentFile file = getOwnedFile(contentId, fileId, requesterUserId, requesterAdmin);
        return storageService.createDownloadUrl(
                file.getObjectName(),
                file.getOriginalName(),
                requesterUserId,
                requesterAdmin
        );
    }

    private StoredObject store(
            LearningContent content,
            ContentFileRole fileRole,
            MultipartFile multipartFile,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        try {
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

    private ContentFile getOwnedFile(
            Long contentId,
            Long fileId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = contentService.getRequired(contentId);
        contentService.assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        ContentFile file = getRequired(fileId);
        if (!contentId.equals(file.getContentId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在");
        }
        return file;
    }

    private ContentFile getRequired(Long fileId) {
        return fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料文件不存在"));
    }

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

    private boolean isReferencedByBody(String articleBody, Long fileId) {
        if (articleBody == null || articleBody.isBlank() || fileId == null) {
            return false;
        }
        return articleBody.contains("content-image://" + fileId)
                || articleBody.contains("content-file://" + fileId);
    }
}
