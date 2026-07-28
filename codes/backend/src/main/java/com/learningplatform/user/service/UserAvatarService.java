/* 文件职责：实现用户头像业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.MinioProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.storage.FileContentSignatureValidator;
import com.learningplatform.common.redis.RedisJsonCache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserAvatar;
import com.learningplatform.user.mapper.UserAvatarMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;

@Service
/**
 * 实现用户头像业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class UserAvatarService {
    private static final TypeReference<String> STRING_TYPE = new TypeReference<>() {
    };
    private static final Logger log = LoggerFactory.getLogger(UserAvatarService.class);
    /** 定义 MAX_AVATAR_BYTES 常量，统一该组件使用的固定规则或默认值。 */
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    /** 访问头像持久化数据。 */
    private final UserAvatarMapper avatarMapper;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 通过minioClient调用隔离后的外部能力。 */
    private final MinioClient minioClient;
    /** 保存minio配置属性，供该类型的业务逻辑读取或更新。 */
    private final MinioProperties minioProperties;
    /** 保存signature校验器，供该类型的业务逻辑读取或更新。 */
    private final FileContentSignatureValidator signatureValidator;
    private final PublicUserProfileCache publicUserProfileCache;
    private final RedisJsonCache redisJsonCache;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public UserAvatarService(
            UserAvatarMapper avatarMapper,
            UserService userService,
            MinioClient minioClient,
            MinioProperties minioProperties,
            FileContentSignatureValidator signatureValidator,
            PublicUserProfileCache publicUserProfileCache,
            RedisJsonCache redisJsonCache
    ) {
        this.avatarMapper = avatarMapper;
        this.userService = userService;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.signatureValidator = signatureValidator;
        this.publicUserProfileCache = publicUserProfileCache;
        this.redisJsonCache = redisJsonCache;
    }

    /** 执行 avatarUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String avatarUrl(User user) {
        if (user == null || user.getId() == null) return null;
        return redisJsonCache.get(
                avatarCacheKey(user.getId()),
                STRING_TYPE,
                Duration.ofMinutes(10),
                () -> avatarMapper.findByUserId(user.getId())
                        .map(this::platformUrl)
                        .orElse(user.getAvatarUrl())
        );
    }

    /** 执行 avatarUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String avatarUrl(Long userId) {
        if (userId == null) return null;
        return redisJsonCache.get(
                avatarCacheKey(userId),
                STRING_TYPE,
                Duration.ofMinutes(10),
                () -> avatarMapper.findByUserId(userId)
                        .map(this::platformUrl)
                        .orElseGet(() -> userService.findById(userId)
                                .map(User::getAvatarUrl)
                                .orElse(null))
        );
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public Optional<UserAvatar> find(Long userId) {
        return avatarMapper.findByUserId(userId);
    }

    /** 返回Required。 */
    public UserAvatar getRequired(Long userId) {
        userService.getRequiredActiveById(userId);
        return find(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户尚未上传头像"));
    }

    /** 执行 open 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public InputStream open(UserAvatar avatar) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(avatar.getBucketName())
                            .object(avatar.getObjectName())
                            .build()
            );
        } catch (Exception exception) {
            log.error("Unable to read avatar object userId={}", avatar.getUserId(), exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像存储服务暂时不可用");
        }
    }

    @Transactional
    /** 执行 upload 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String upload(Long userId, MultipartFile file) {
        userService.getRequiredById(userId);
        ValidatedAvatar validated = validate(file);
        String objectName = "avatar/%d/%s.%s".formatted(
                userId,
                UUID.randomUUID(),
                validated.extension()
        );
        UserAvatar previous = avatarMapper.findByUserId(userId).orElse(null);

        try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
            input.mark(512);
            byte[] header = input.readNBytes(512);
            input.reset();
            signatureValidator.validate(validated.extension(), header);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .stream(input, file.getSize(), -1L)
                            .contentType(validated.contentType())
                            .build()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Unable to upload avatar userId={} object={}", userId, objectName, exception);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "头像上传失败，请稍后重试");
        }

        UserAvatar avatar = new UserAvatar();
        avatar.setUserId(userId);
        avatar.setBucketName(minioProperties.bucket());
        avatar.setObjectName(objectName);
        avatar.setOriginalName(validated.originalName());
        avatar.setContentType(validated.contentType());
        avatar.setExtension(validated.extension());
        avatar.setSizeBytes(file.getSize());
        try {
            if (avatarMapper.upsert(avatar) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存头像信息失败");
            }
        } catch (RuntimeException exception) {
            removeQuietly(minioProperties.bucket(), objectName);
            throw exception;
        }

        if (userService.getRequiredById(userId).getAvatarUrl() != null) {
            userService.clearAvatarUrl(userId);
        }
        if (previous != null && !previous.getObjectName().equals(objectName)) {
            removeQuietly(previous.getBucketName(), previous.getObjectName());
        }
        publicUserProfileCache.evictAfterCommit(userId);
        redisJsonCache.evictAfterCommit(avatarCacheKey(userId));
        return platformUrl(avatarMapper.findByUserId(userId).orElse(avatar));
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long userId) {
        UserAvatar avatar = avatarMapper.findByUserId(userId).orElse(null);
        if (avatar != null) {
            if (avatarMapper.deleteByUserId(userId) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "头像状态已变化，请刷新后重试");
            }
            removeQuietly(avatar.getBucketName(), avatar.getObjectName());
        }
        if (userService.getRequiredById(userId).getAvatarUrl() != null) {
            userService.clearAvatarUrl(userId);
        }
        publicUserProfileCache.evictAfterCommit(userId);
        redisJsonCache.evictAfterCommit(avatarCacheKey(userId));
    }

    /** 校验及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ValidatedAvatar validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "头像图片不能超过 5 MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()
                || originalName.length() > 255
                || originalName.contains("/") || originalName.contains("\\")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "头像文件名不合法");
        }
        int dot = originalName.lastIndexOf('.');
        String extension = dot < 1 || dot == originalName.length() - 1
                ? ""
                : originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedType = ALLOWED_TYPES.get(extension);
        String actualType = file.getContentType() == null
                ? ""
                : file.getContentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (expectedType == null || !expectedType.equals(actualType)) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "头像仅支持 JPG、PNG、WebP 图片，且文件类型必须正确"
            );
        }
        return new ValidatedAvatar(originalName.trim(), extension, actualType);
    }

    /** 执行 platformUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String platformUrl(UserAvatar avatar) {
        int version = avatar.getUpdatedAt() == null ? 0 : avatar.getUpdatedAt().hashCode();
        return "/api/users/%d/avatar?v=%s".formatted(
                avatar.getUserId(),
                Integer.toUnsignedString(version)
        );
    }

    /** 删除、移除或清理Quietly，同时维护关联数据和权限不变量。 */
    private void removeQuietly(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception exception) {
            log.warn("Unable to remove replaced avatar object={}", objectName, exception);
        }
    }

    /** 校验d头像及相关业务前置条件，不满足时抛出明确业务异常。 */
    private record ValidatedAvatar(String originalName, String extension, String contentType) {
    }

    private String avatarCacheKey(Long userId) {
        return "lp:v1:user:avatar-url:" + userId;
    }
}
