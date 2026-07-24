package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StorageObjectKeyFactory {
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "^content/([1-9][0-9]*)/[0-9]{4}/(?:0[1-9]|1[0-2])/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                    + "\\.[a-z0-9]{1,10}$"
    );

    private final Clock clock;

    public StorageObjectKeyFactory() {
        this(Clock.systemUTC());
    }

    StorageObjectKeyFactory(Clock clock) {
        this.clock = clock;
    }

    public String create(long ownerId, String extension) {
        if (ownerId <= 0 || extension == null || !extension.matches("[a-z0-9]{1,10}")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法生成安全的文件对象名");
        }
        LocalDate today = LocalDate.now(clock);
        return "content/%d/%04d/%02d/%s.%s".formatted(
                ownerId,
                today.getYear(),
                today.getMonthValue(),
                UUID.randomUUID(),
                extension
        );
    }

    public long ownerId(String objectName) {
        if (objectName == null) {
            throw invalidObjectName();
        }
        Matcher matcher = OBJECT_KEY_PATTERN.matcher(objectName);
        if (!matcher.matches()) {
            throw invalidObjectName();
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw invalidObjectName();
        }
    }

    public void assertCanAccess(String objectName, long requesterUserId, boolean requesterAdmin) {
        if (requesterUserId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前用户无效");
        }
        long ownerId = ownerId(objectName);
        if (!requesterAdmin && ownerId != requesterUserId) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问其他用户的文件");
        }
    }

    private BusinessException invalidObjectName() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "文件对象名不合法");
    }
}
