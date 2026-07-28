/* 文件职责：校验文件学习资料Signature校验器的格式和业务不变量，失败时返回明确错误。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：对象存储层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
/**
 * 校验文件学习资料Signature校验器的格式和业务不变量，失败时返回明确错误。
 *
 * <p>职责边界：对象存储保持私有，外部访问只能使用受控的短期签名地址。</p>
 */
public class FileContentSignatureValidator {
    /** 校验及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void validate(String extension, byte[] header) {
        String normalized = extension == null
                ? ""
                : extension.toLowerCase(Locale.ROOT);
        boolean valid = switch (normalized) {
            case "jpg", "jpeg" -> startsWith(
                    header,
                    0xFF, 0xD8, 0xFF
            );
            case "png" -> startsWith(
                    header,
                    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            );
            case "webp" -> asciiAt(header, 0, "RIFF")
                    && asciiAt(header, 8, "WEBP");
            case "pdf" -> asciiAt(header, 0, "%PDF-");
            case "zip", "docx", "pptx", "xlsx" -> isZip(header);
            case "doc", "ppt", "xls" -> startsWith(
                    header,
                    0xD0, 0xCF, 0x11, 0xE0,
                    0xA1, 0xB1, 0x1A, 0xE1
            );
            case "mp4" -> asciiAt(header, 4, "ftyp");
            case "webm" -> startsWith(
                    header,
                    0x1A, 0x45, 0xDF, 0xA3
            );
            case "txt", "md", "srt", "vtt" -> looksLikeText(header);
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "文件内容与扩展名或 MIME 类型不匹配"
            );
        }
    }

    /** 判断是否满足Zip条件，不修改持久化状态。 */
    private boolean isZip(byte[] header) {
        return startsWith(header, 0x50, 0x4B, 0x03, 0x04)
                || startsWith(header, 0x50, 0x4B, 0x05, 0x06)
                || startsWith(header, 0x50, 0x4B, 0x07, 0x08);
    }

    /** 执行 looksLikeText 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private boolean looksLikeText(byte[] header) {
        if (header == null || header.length == 0) {
            return false;
        }
        for (byte value : header) {
            int unsigned = Byte.toUnsignedInt(value);
            if (unsigned == 0) {
                return false;
            }
            if (unsigned < 0x09
                    || (unsigned > 0x0D && unsigned < 0x20)
                    || unsigned == 0x7F) {
                return false;
            }
        }
        String decoded = new String(header, StandardCharsets.UTF_8);
        return !decoded.contains("\uFFFD");
    }

    /** 执行 asciiAt 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private boolean asciiAt(byte[] header, int offset, String value) {
        if (header == null || offset < 0
                || header.length < offset + value.length()) {
            return false;
        }
        byte[] expected = value.getBytes(StandardCharsets.US_ASCII);
        for (int index = 0; index < expected.length; index++) {
            if (header[offset + index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    /** 创建或初始化sWith，并维护唯一性、初始状态和必要关联。 */
    private boolean startsWith(byte[] header, int... expected) {
        if (header == null || header.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (Byte.toUnsignedInt(header[index]) != expected[index]) {
                return false;
            }
        }
        return true;
    }
}
