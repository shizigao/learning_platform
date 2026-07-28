/* 文件职责：从输入中提取并规范化学习资料Text提取器，为后续业务或 AI 调用提供安全文本。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：文本提取与规范化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 从输入中提取并规范化学习资料Text提取器，为后续业务或 AI 调用提供安全文本。
 *
 * <p>职责边界：遵守 AI 任务、对话、分析与供应商调用 模块的职责边界。</p>
 */
public class ContentTextExtractor {
    /** 定义 MAX_TEXT_FILE_BYTES 常量，统一该组件使用的固定规则或默认值。 */
    private static final int MAX_TEXT_FILE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md");
    /** 定义 TEXT_FILE_ROLES 常量，统一该组件使用的固定规则或默认值。 */
    private static final Set<ContentFileRole> TEXT_FILE_ROLES =
            Set.of(ContentFileRole.CONTENT, ContentFileRole.ATTACHMENT);

    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;
    /** 访问文件持久化数据。 */
    private final ContentFileMapper fileMapper;
    /** 委托存储执行对应领域规则。 */
    private final MinioStorageService storageService;
    /** 保存最大输入Chars，供该类型的业务逻辑读取或更新。 */
    private final int maxInputChars;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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

    /** 执行 extract 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ExtractedContentText extract(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        // 检查该该用户是否有获取该资料的资格，如果有则取出，点击requireAccess
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

    /** 执行 supports 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 查询Utf8相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 执行 appendSection 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 执行 sha256 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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
