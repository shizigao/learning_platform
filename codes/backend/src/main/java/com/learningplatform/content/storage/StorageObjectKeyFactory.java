/* 文件职责：集中创建存储Object键工厂，保证不同调用场景使用一致规则。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：对象存储层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 集中创建存储Object键工厂，保证不同调用场景使用一致规则。
 *
 * <p>职责边界：对象存储保持私有，外部访问只能使用受控的短期签名地址。</p>
 */
public class StorageObjectKeyFactory {
    private static final Pattern OBJECT_KEY_PATTERN = Pattern.compile(
            "^content/([1-9][0-9]*)/[0-9]{4}/(?:0[1-9]|1[0-2])/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"
                    + "\\.[a-z0-9]{1,10}$"
    );

    /** 提供可替换时间源，便于测试时间相关规则。 */
    private final Clock clock;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public StorageObjectKeyFactory() {
        this(Clock.systemUTC());
    }

    StorageObjectKeyFactory(Clock clock) {
        this.clock = clock;
    }

    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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

    /** 执行 ownerId 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 校验Can访问权及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void assertCanAccess(String objectName, long requesterUserId, boolean requesterAdmin) {
        if (requesterUserId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前用户无效");
        }
        long ownerId = ownerId(objectName);
        if (!requesterAdmin && ownerId != requesterUserId) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问其他用户的文件");
        }
    }

    /** 执行 invalidObjectName 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private BusinessException invalidObjectName() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "文件对象名不合法");
    }
}
