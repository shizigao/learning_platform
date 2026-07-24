package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileContentSignatureValidatorTests {
    private final FileContentSignatureValidator validator =
            new FileContentSignatureValidator();

    @Test
    void acceptsKnownBinaryAndTextSignatures() {
        validator.validate("jpg", bytes(
                0xFF, 0xD8, 0xFF, 0xE0
        ));
        validator.validate("png", bytes(
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        ));
        validator.validate(
                "pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)
        );
        validator.validate(
                "docx",
                bytes(0x50, 0x4B, 0x03, 0x04)
        );
        validator.validate(
                "txt",
                "安全的 UTF-8 文本".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void rejectsExecutableRenamedAsAllowedFile() {
        byte[] executable = new byte[]{
                'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0
        };

        assertThatThrownBy(() -> validator.validate("jpg", executable))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> {
                            assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.BAD_REQUEST);
                            assertThat(exception.getMessage())
                                    .contains("文件内容与扩展名");
                        }
                );
        assertThatThrownBy(() -> validator.validate("txt", executable))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsUnknownExtensionAndMalformedSignature() {
        assertThatThrownBy(() -> validator.validate(
                "exe",
                bytes(0x4D, 0x5A)
        )).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> validator.validate(
                "mp4",
                "not-an-mp4".getBytes(StandardCharsets.US_ASCII)
        )).isInstanceOf(BusinessException.class);
    }

    private byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = (byte) values[index];
        }
        return result;
    }
}
