package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.storage.MinioStorageService;
import com.learningplatform.classroom.mapper.ClassScopeMapper;
import com.learningplatform.order.service.EntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentAccessServiceTests {
    private LearningContentMapper contentMapper;
    private ContentFileMapper fileMapper;
    private EntitlementService entitlementService;
    private MinioStorageService storageService;
    private ClassScopeMapper classScopeMapper;
    private ContentAccessService accessService;
    private LearningContent paidContent;
    private ContentFile contentFile;

    @BeforeEach
    void setUp() {
        contentMapper = mock(LearningContentMapper.class);
        fileMapper = mock(ContentFileMapper.class);
        entitlementService = mock(EntitlementService.class);
        storageService = mock(MinioStorageService.class);
        classScopeMapper = mock(ClassScopeMapper.class);
        accessService = new ContentAccessService(
                contentMapper,
                fileMapper,
                entitlementService,
                storageService,
                classScopeMapper
        );

        paidContent = new LearningContent();
        paidContent.setId(100L);
        paidContent.setPublisherId(10L);
        paidContent.setFree(false);
        paidContent.setStatus(ContentStatus.PUBLISHED);
        when(contentMapper.findById(100L)).thenReturn(Optional.of(paidContent));

        contentFile = new ContentFile();
        contentFile.setId(200L);
        contentFile.setContentId(100L);
        contentFile.setObjectName("content/10/2026/07/123e4567-e89b-42d3-a456-426614174000.pdf");
        contentFile.setOriginalName("course.pdf");
        when(fileMapper.findById(200L)).thenReturn(Optional.of(contentFile));
    }

    @Test
    void blocksRealFileUrlWithoutEntitlement() {
        when(entitlementService.hasActiveContentAccess(20L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> accessService.previewUrl(100L, 200L, 20L, false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(storageService, never()).createAuthorizedPreviewUrl(contentFile.getObjectName());
    }

    @Test
    void returnsShortLivedFileUrlAfterEntitlementCheck() {
        when(entitlementService.hasActiveContentAccess(20L, 100L)).thenReturn(true);
        when(storageService.createAuthorizedDownloadUrl(
                contentFile.getObjectName(),
                contentFile.getOriginalName()
        )).thenReturn("http://localhost:9000/signed-download");

        assertThat(accessService.downloadUrl(100L, 200L, 20L, false))
                .isEqualTo("http://localhost:9000/signed-download");
    }

    @Test
    void blocksFileIdThatDoesNotBelongToRequestedContent() {
        when(entitlementService.hasActiveContentAccess(20L, 100L)).thenReturn(true);
        contentFile.setContentId(101L);

        assertThatThrownBy(() -> accessService.downloadUrl(100L, 200L, 20L, false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(storageService, never()).createAuthorizedDownloadUrl(
                contentFile.getObjectName(),
                contentFile.getOriginalName()
        );
    }
}
