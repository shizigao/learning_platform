package com.learningplatform.offline.security;

import com.learningplatform.common.config.TeacherDataSecurityProperties;
import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TeacherSensitiveDataCryptoTests {
    private final TeacherSensitiveDataCrypto crypto =
            new TeacherSensitiveDataCrypto(new TeacherDataSecurityProperties(
                    Base64.getEncoder().encodeToString(new byte[32])
            ));

    @Test
    void encryptsWithRandomIvAndDecryptsIdentity() {
        String identity = "11010519491231002X";

        var first = crypto.encryptIdentity(identity);
        var second = crypto.encryptIdentity(identity);

        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
        assertThat(first.iv()).isNotEqualTo(second.iv());
        assertThat(first.hmac()).isEqualTo(second.hmac());
        assertThat(first.masked()).startsWith("1101").endsWith("002X");
        assertThat(crypto.decryptIdentity(first.ciphertext(), first.iv()))
                .isEqualTo(identity);
    }

    @Test
    void rejectsTamperedCiphertext() {
        var encrypted = crypto.encryptIdentity("11010519491231002X");
        encrypted.ciphertext()[0] ^= 1;

        assertThatThrownBy(() -> crypto.decryptIdentity(
                encrypted.ciphertext(),
                encrypted.iv()
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("无法解密");
    }
}
