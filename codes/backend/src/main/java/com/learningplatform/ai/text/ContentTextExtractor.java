package com.learningplatform.ai.text;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.AiProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.content.storage.MinioStorageService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ContentTextExtractor {
    private static final int MAX_TEXT_FILE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md");
    private static final Set<ContentFileRole> TEXT_FILE_ROLES =
            Set.of(ContentFileRole.CONTENT, ContentFileRole.ATTACHMENT);

    private final ContentAccessService accessService;
    private final ContentFileMapper fileMapper;
    private final MinioStorageService storageService;
    private final int maxInputChars;

    public ContentTextExtractor(
            ContentAccessService accessService,
            ContentFileMapper fileMapper,
            MinioStorageService storageService,
            AiProperties aiProperties
    ) {
        this.accessService = accessService;
        this.fileMapper = fileMapper;
        this.storageService = storageService;
        if (aiProperties == null
                || aiProperties.limits() == null
                || aiProperties.limits().maxInputChars() <= 0) {
            throw new IllegalStateException("AI 最大输入长度配置无效");
        }
        this.maxInputChars = aiProperties.limits().maxInputChars();
    }

    public ExtractedContentText extract(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = accessService.requireAccess(
                contentId,
                userId,
                requesterAdmin
        );
        StringBuilder text = new StringBuilder();
        appendSection(text, "资料标题", content.getTitle());
        appendSection(text, "资料简介", content.getSummary());
        appendSection(text, "图文正文", content.getArticleBody());

        List<String> includedFiles = new ArrayList<>();
        for (ContentFile file : fileMapper.findByContentId(contentId)) {
            if (!supports(file)) {
                continue;
            }
            String fileText = readUtf8(file);
            if (fileText == null || fileText.isBlank()) {
                continue;
            }
            appendSection(text, "文本文件：" + file.getOriginalName(), fileText);
            includedFiles.add(file.getOriginalName());
        }

        String normalized = text.toString().trim();
        if (normalized.isBlank()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "该资料暂无可供 AI 使用的文本内容"
            );
        }
        return new ExtractedContentText(
                contentId,
                content.getTitle(),
                normalized,
                sha256(normalized),
                includedFiles
        );
    }

    private boolean supports(ContentFile file) {
        if (file.getFileRole() == null
                || !TEXT_FILE_ROLES.contains(file.getFileRole())
                || file.getExtension() == null
                || file.getSizeBytes() == null
                || file.getSizeBytes() <= 0
                || file.getSizeBytes() > MAX_TEXT_FILE_BYTES) {
            return false;
        }
        return TEXT_EXTENSIONS.contains(
                file.getExtension().trim().toLowerCase(Locale.ROOT)
        );
    }

    private String readUtf8(ContentFile file) {
        try (InputStream input = storageService.downloadAuthorized(file.getObjectName())) {
            byte[] bytes = input.readNBytes(MAX_TEXT_FILE_BYTES + 1);
            if (bytes.length > MAX_TEXT_FILE_BYTES) {
                return null;
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            return null;
        } catch (IOException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "读取资料文本文件失败"
            );
        }
    }

    private void appendSection(StringBuilder target, String heading, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        int separatorLength = target.isEmpty() ? 0 : 2;
        int requiredLength = separatorLength
                + 3
                + heading.length()
                + 1
                + value.trim().length();
        if (target.length() + requiredLength > maxInputChars) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "资料文本超过 AI 输入长度限制"
            );
        }
        if (!target.isEmpty()) {
            target.append("\n\n");
        }
        target.append("## ").append(heading).append('\n').append(value.trim());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
