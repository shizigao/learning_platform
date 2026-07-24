package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class FileContentSignatureValidator {
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

    private boolean isZip(byte[] header) {
        return startsWith(header, 0x50, 0x4B, 0x03, 0x04)
                || startsWith(header, 0x50, 0x4B, 0x05, 0x06)
                || startsWith(header, 0x50, 0x4B, 0x07, 0x08);
    }

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
