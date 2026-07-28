/* 文件职责：表示教师敏感配置DataCrypto领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 表示教师敏感配置DataCrypto领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class TeacherSensitiveDataCrypto {
    /** 定义 GCM_TAG_BITS 常量，统一该组件使用的固定规则或默认值。 */
    private static final int GCM_TAG_BITS = 128;
    /** 定义 IV_BYTES 常量，统一该组件使用的固定规则或默认值。 */
    private static final int IV_BYTES = 12;
    /** 保存配置属性，供该类型的业务逻辑读取或更新。 */
    private final TeacherDataSecurityProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public TeacherSensitiveDataCrypto(TeacherDataSecurityProperties properties) {
        this.properties = properties;
    }

    /** 执行 encryptIdentity 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 执行 decryptIdentity 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 执行 hmac 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private byte[] hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derive("teacher-id-hmac"), "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 执行 derive 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private byte[] derive(String purpose) throws Exception {
        byte[] base = key();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(base);
        digest.update((byte) 0);
        digest.update(purpose.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    /** 执行 key 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 转换或规范化Identity数据，不引入额外持久化副作用。 */
    private String normalizeIdentity(String value) {
        if (value == null) return "";
        return value.replace(" ", "").toUpperCase(Locale.ROOT);
    }

    /** 执行 mask 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String mask(String value) {
        if (value.length() <= 8) return "****";
        return value.substring(0, 4)
                + "*".repeat(Math.max(4, value.length() - 8))
                + value.substring(value.length() - 4);
    }

    /** 执行 EncryptedIdentity 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record EncryptedIdentity(
            byte[] ciphertext,
            byte[] iv,
            byte[] hmac,
            String masked
    ) {
    }
}
