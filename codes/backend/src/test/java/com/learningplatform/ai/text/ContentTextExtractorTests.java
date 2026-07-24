package com.learningplatform.ai.text;

import com.learningplatform.common.config.AiProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.content.storage.MinioStorageService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentTextExtractorTests {
    @Test
    void extractsArticleAndUtf8TextFilesWhileIgnoringBinaryMedia() {
        ContentAccessService accessService = mock(ContentAccessService.class);
        ContentFileMapper fileMapper = mock(ContentFileMapper.class);
        MinioStorageService storageService = mock(MinioStorageService.class);
        ContentTextExtractor extractor = new ContentTextExtractor(
                accessService,
                fileMapper,
                storageService,
                aiProperties(100_000)
        );
        LearningContent content = new LearningContent();
        content.setId(10L);
        content.setTitle("数据库事务");
        content.setSummary("事务用于保证数据一致性");
        content.setArticleBody("ACID 包括原子性、一致性、隔离性和持久性。");
        content.setStatus(ContentStatus.PUBLISHED);
        when(accessService.requireAccess(10L, 1L, false)).thenReturn(content);

        ContentFile markdown = file(
                10L,
                ContentFileRole.CONTENT,
                "补充说明.md",
                "md",
                "content/2/2026/07/text.md",
                32L
        );
        ContentFile video = file(
                10L,
                ContentFileRole.VIDEO,
                "课程.mp4",
                "mp4",
                "content/2/2026/07/video.mp4",
                1024L
        );
        ContentFile image = file(
                10L,
                ContentFileRole.COVER,
                "封面.png",
                "png",
                "content/2/2026/07/cover.png",
                1024L
        );
        when(fileMapper.findByContentId(10L))
                .thenReturn(List.of(markdown, video, image));
        when(storageService.downloadAuthorized(markdown.getObjectName()))
                .thenReturn(new ByteArrayInputStream(
                        "隔离级别用于控制并发事务。".getBytes(StandardCharsets.UTF_8)
                ));

        ExtractedContentText result = extractor.extract(10L, 1L, false);

        assertThat(result.text())
                .contains("数据库事务", "ACID", "隔离级别")
                .doesNotContain("课程.mp4", "封面.png");
        assertThat(result.includedTextFiles()).containsExactly("补充说明.md");
        assertThat(result.sourceVersion()).hasSize(64);
        verify(storageService).downloadAuthorized(markdown.getObjectName());
        verify(storageService, never()).downloadAuthorized(video.getObjectName());
        verify(storageService, never()).downloadAuthorized(image.getObjectName());
    }

    @Test
    void rejectsContentThatExceedsConfiguredInputLimit() {
        ContentAccessService accessService = mock(ContentAccessService.class);
        ContentFileMapper fileMapper = mock(ContentFileMapper.class);
        MinioStorageService storageService = mock(MinioStorageService.class);
        ContentTextExtractor extractor = new ContentTextExtractor(
                accessService,
                fileMapper,
                storageService,
                aiProperties(20)
        );
        LearningContent content = new LearningContent();
        content.setId(11L);
        content.setTitle("这是一段明显超过限制的资料标题");
        when(accessService.requireAccess(11L, 1L, false)).thenReturn(content);

        assertThatThrownBy(() -> extractor.extract(11L, 1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资料文本超过 AI 输入长度限制");
        verify(storageService, never()).downloadAuthorized(
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    private AiProperties aiProperties(int maxInputChars) {
        return new AiProperties(
                "mock",
                new AiProperties.MockProvider("test", "success", Duration.ZERO),
                null,
                new AiProperties.Limits(
                        maxInputChars,
                        10,
                        20_000,
                        10,
                        Duration.ofMinutes(1),
                        1,
                        Duration.ofSeconds(5)
                )
        );
    }

    private ContentFile file(
            Long contentId,
            ContentFileRole role,
            String name,
            String extension,
            String objectName,
            Long size
    ) {
        ContentFile file = new ContentFile();
        file.setContentId(contentId);
        file.setFileRole(role);
        file.setOriginalName(name);
        file.setExtension(extension);
        file.setObjectName(objectName);
        file.setSizeBytes(size);
        return file;
    }
}
