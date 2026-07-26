package com.learningplatform.offline.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.TeacherDataSecurityProperties;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

@Service
public class TeacherSensitiveDataCrypto {
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private final TeacherDataSecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TeacherSensitiveDataCrypto(TeacherDataSecurityProperties properties) {
        this.properties = properties;
    }

    public EncryptedIdentity encryptIdentity(String rawIdentity) {
        String normalized = normalizeIdentity(rawIdentity);
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(derive("teacher-id-aes"), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );
            byte[] ciphertext = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return new EncryptedIdentity(
                    ciphertext,
                    iv,
                    hmac(normalized),
                    mask(normalized)
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "教师身份信息加密失败");
        }
    }

    public String decryptIdentity(byte[] ciphertext, byte[] iv) {
        if (ciphertext == null || iv == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "教师身份信息不完整");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(derive("teacher-id-aes"), "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "教师身份信息无法解密，请检查本机加密密钥"
            );
        }
    }

    private byte[] hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derive("teacher-id-hmac"), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] derive(String purpose) throws Exception {
        byte[] base = key();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(base);
        digest.update((byte) 0);
        digest.update(purpose.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    private byte[] key() {
        String encoded = properties.encryptionKey();
        if (encoded == null || encoded.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "未配置教师敏感数据加密密钥"
            );
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded.trim());
            if (decoded.length != 32) {
                throw new IllegalArgumentException("length");
            }
            return Arrays.copyOf(decoded, decoded.length);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "教师敏感数据加密密钥格式无效"
            );
        }
    }

    private String normalizeIdentity(String value) {
        if (value == null) return "";
        return value.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String mask(String value) {
        if (value.length() <= 8) return "****";
        return value.substring(0, 4)
                + "*".repeat(Math.max(4, value.length() - 8))
                + value.substring(value.length() - 4);
    }

    public record EncryptedIdentity(
            byte[] ciphertext,
            byte[] iv,
            byte[] hmac,
            String masked
    ) {
    }
}
