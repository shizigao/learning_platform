package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageObjectKeyFactoryTests {
    private final StorageObjectKeyFactory factory = new StorageObjectKeyFactory(
            Clock.fixed(Instant.parse("2026-07-23T06:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createsOpaqueOwnerScopedObjectKey() {
        String objectName = factory.create(42, "pdf");

        assertThat(objectName)
                .matches("content/42/2026/07/[0-9a-f-]{36}\\.pdf");
        assertThat(factory.ownerId(objectName)).isEqualTo(42);
    }

    @Test
    void rejectsMalformedAndCrossOwnerObjectKeys() {
        assertThatThrownBy(() -> factory.ownerId("../content/42/file.pdf"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        String objectName = factory.create(42, "pdf");
        assertThatThrownBy(() -> factory.assertCanAccess(objectName, 41, false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatCode(() -> factory.assertCanAccess(objectName, 41, true))
                .doesNotThrowAnyException();
    }
}
