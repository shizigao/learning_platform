/* 文件职责：实现线下教学教师业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.offline.domain.OfflineTeacherApplication;
import com.learningplatform.offline.domain.OfflineTeacherProfile;
import com.learningplatform.offline.domain.TeacherApplicationAdminView;
import com.learningplatform.offline.domain.TeacherApplicationStatus;
import com.learningplatform.offline.domain.TeacherProfileStatus;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationRequest;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationResponse;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationSummary;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherProfileResponse;
import com.learningplatform.offline.dto.TeacherApplicationAdminQuery;
import com.learningplatform.offline.dto.TeacherSearchQuery;
import com.learningplatform.offline.mapper.OfflineTeachingMapper;
import com.learningplatform.offline.security.TeacherSensitiveDataCrypto;
import com.learningplatform.offline.security.TeacherSensitiveDataCrypto.EncryptedIdentity;
import com.learningplatform.user.service.UserAvatarService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
/**
 * 实现线下教学教师业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class OfflineTeacherService {
    /** 保存mapper，供该类型的业务逻辑读取或更新。 */
    private final OfflineTeachingMapper mapper;
    /** 保存crypto，供该类型的业务逻辑读取或更新。 */
    private final TeacherSensitiveDataCrypto crypto;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OfflineTeacherService(
            OfflineTeachingMapper mapper,
            TeacherSensitiveDataCrypto crypto,
            UserAvatarService avatarService,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.crypto = crypto;
        this.avatarService = avatarService;
        this.objectMapper = objectMapper;
    }

    /** 执行 currentApplication 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public Optional<TeacherApplicationResponse> currentApplication(Long userId) {
        return mapper.findApplicationByUserId(userId)
                .map(application -> applicationResponse(application, false));
    }

    @Transactional
    /** 更新申请，通过返回值或版本条件识别并发状态变化。 */
    public TeacherApplicationResponse saveApplication(
            Long userId,
            TeacherApplicationRequest request
    ) {
        requireContact(request.contactWechat(), request.contactQq(), request.contactEmail());
        EncryptedIdentity identity = crypto.encryptIdentity(request.idCardNumber());
        OfflineTeacherApplication existing =
                mapper.findApplicationByUserId(userId).orElse(null);
        OfflineTeacherApplication application = application(request, userId, identity);
        try {
            if (existing == null) {
                if (mapper.insertApplication(application) != 1) {
                    throw internal("保存教师申请失败");
                }
            } else {
                application.setId(existing.getId());
                if (mapper.updateApplication(application) != 1) {
                    throw new BusinessException(
                            ErrorCode.CONFLICT,
                            "教师申请状态已变化，请刷新后重试"
                    );
                }
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "该身份证号已用于其他教师申请"
            );
        }
        return applicationResponse(
                mapper.findApplicationByUserId(userId)
                        .orElseThrow(() -> internal("保存后无法读取教师申请")),
                false
        );
    }

    @Transactional
    /** 执行提交状态流转，仅允许从合法前置状态进入目标状态。 */
    public TeacherApplicationResponse submit(Long userId) {
        OfflineTeacherApplication application = mapper.findApplicationByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "请先填写并保存教师信息"
                ));
        if (application.getStatus() != TeacherApplicationStatus.DRAFT) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    application.getStatus() == TeacherApplicationStatus.PENDING
                            ? "教师申请正在审核中"
                            : "请先保存修改后的教师信息再提交"
            );
        }
        if (mapper.submitApplication(application.getId(), userId, now()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "教师申请状态已变化");
        }
        return applicationResponse(
                mapper.findApplicationByUserId(userId).orElseThrow(),
                false
        );
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<TeacherProfileResponse> search(TeacherSearchQuery query) {
        long total = mapper.countProfiles(
                query.getKeyword(),
                query.getProvince(),
                query.getCity(),
                query.getTeachingTag(),
                query.getMaxHourlyRate()
        );
        List<TeacherProfileResponse> items = mapper.findProfiles(
                        query.getKeyword(),
                        query.getProvince(),
                        query.getCity(),
                        query.getTeachingTag(),
                        query.getMaxHourlyRate(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::profileResponse)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 执行 publicProfile 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public TeacherProfileResponse publicProfile(Long profileId) {
        OfflineTeacherProfile profile = mapper.findProfileById(profileId)
                .filter(value -> value.getStatus() == TeacherProfileStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "线下教师不存在或已暂停展示"
                ));
        return profileResponse(profile);
    }

    /** 执行 publicProfileByUser 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public TeacherProfileResponse publicProfileByUser(Long userId) {
        OfflineTeacherProfile profile = mapper.findProfileByUserId(userId)
                .filter(value -> value.getStatus() == TeacherProfileStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "该用户尚未成为线下教师"
                ));
        return profileResponse(profile);
    }

    /** 执行 adminProfileByUser 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public TeacherProfileResponse adminProfileByUser(Long userId) {
        return profileResponse(
                mapper.findProfileByUserId(userId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.NOT_FOUND,
                                "该用户尚无公开教师资料"
                        ))
        );
    }

    /** 执行 adminApplications 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public PageResult<TeacherApplicationSummary> adminApplications(
            TeacherApplicationAdminQuery query
    ) {
        long total = mapper.countApplicationsForAdmin(
                query.getStatus(),
                query.getKeyword()
        );
        List<TeacherApplicationSummary> items = mapper.findApplicationsForAdmin(
                        query.getStatus(),
                        query.getKeyword(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::applicationSummary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 执行 adminApplication 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public TeacherApplicationResponse adminApplication(Long applicationId) {
        return applicationResponse(
                mapper.findApplicationById(applicationId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.NOT_FOUND,
                                "教师申请不存在"
                        )),
                true
        );
    }

    @Transactional
    /** 执行审核通过状态流转，仅允许从合法前置状态进入目标状态。 */
    public TeacherApplicationResponse approve(Long applicationId, Long adminId) {
        LocalDateTime reviewedAt = now();
        if (mapper.reviewApplication(
                applicationId,
                TeacherApplicationStatus.APPROVED,
                null,
                reviewedAt,
                adminId
        ) != 1) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "仅待审核申请可以通过"
            );
        }
        if (mapper.upsertProfileFromApplication(
                applicationId,
                reviewedAt,
                adminId
        ) <= 0) {
            throw internal("生成公开教师资料失败");
        }
        return adminApplication(applicationId);
    }

    @Transactional
    /** 执行驳回状态流转，仅允许从合法前置状态进入目标状态。 */
    public TeacherApplicationResponse reject(
            Long applicationId,
            Long adminId,
            String reason
    ) {
        String normalized = requiredReason(reason, "请填写驳回原因");
        if (mapper.reviewApplication(
                applicationId,
                TeacherApplicationStatus.REJECTED,
                normalized,
                now(),
                adminId
        ) != 1) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "仅待审核申请可以驳回"
            );
        }
        return adminApplication(applicationId);
    }

    /** 更新资料状态，通过返回值或版本条件识别并发状态变化。 */
    public TeacherProfileResponse updateProfileStatus(
            Long profileId,
            TeacherProfileStatus status,
            String reason
    ) {
        String normalizedReason = status == TeacherProfileStatus.SUSPENDED
                ? requiredReason(reason, "请填写暂停展示原因")
                : null;
        if (mapper.updateProfileStatus(profileId, status, normalizedReason) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线下教师不存在");
        }
        return profileResponse(
                mapper.findProfileById(profileId).orElseThrow()
        );
    }

    /** 执行推荐Candidates核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public List<OfflineTeacherProfile> recommendationCandidates(
            String province,
            String city,
            java.math.BigDecimal maxRate
    ) {
        // 寻找推荐候选老师，点击findRecommendationCandidates
        return mapper.findRecommendationCandidates(province, city, maxRate);
    }

    /** 执行 profileResponse 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public TeacherProfileResponse profileResponse(OfflineTeacherProfile profile) {
        return new TeacherProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getUsername(),
                profile.getNickname(),
                avatarService.avatarUrl(profile.getUserId()),
                profile.getTeacherName(),
                profile.getGender(),
                profile.getEducationLevel(),
                profile.getEducationBackground(),
                profile.getInstitution(),
                profile.getProvince(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getBio(),
                profile.getTeachingContent(),
                tags(profile.getTeachingTags()),
                profile.getAvailability(),
                profile.getHourlyRate(),
                profile.getPriceDescription(),
                profile.getContactWechat(),
                profile.getContactQq(),
                profile.getContactEmail(),
                profile.getStatus(),
                profile.getSuspendedReason(),
                profile.getApprovedAt()
        );
    }

    /** 执行 application 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private OfflineTeacherApplication application(
            TeacherApplicationRequest request,
            Long userId,
            EncryptedIdentity identity
    ) {
        OfflineTeacherApplication application = new OfflineTeacherApplication();
        application.setUserId(userId);
        application.setTeacherName(clean(request.teacherName()));
        application.setIdCardCiphertext(identity.ciphertext());
        application.setIdCardIv(identity.iv());
        application.setIdCardHmac(identity.hmac());
        application.setIdCardMasked(identity.masked());
        application.setGender(request.gender());
        application.setEducationLevel(request.educationLevel());
        application.setEducationBackground(clean(request.educationBackground()));
        application.setInstitution(optional(request.institution()));
        application.setProvince(clean(request.province()));
        application.setCity(clean(request.city()));
        application.setDistrict(optional(request.district()));
        application.setBio(clean(request.bio()));
        application.setTeachingContent(clean(request.teachingContent()));
        application.setTeachingTags(json(normalizeTags(request.teachingTags())));
        application.setAvailability(clean(request.availability()));
        application.setHourlyRate(request.hourlyRate());
        application.setPriceDescription(optional(request.priceDescription()));
        application.setContactWechat(optional(request.contactWechat()));
        application.setContactQq(optional(request.contactQq()));
        application.setContactEmail(optional(request.contactEmail()));
        application.setStatus(TeacherApplicationStatus.DRAFT);
        application.setVersion(0);
        return application;
    }

    /** 执行 applicationResponse 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private TeacherApplicationResponse applicationResponse(
            OfflineTeacherApplication application,
            boolean revealIdentity
    ) {
        return new TeacherApplicationResponse(
                application.getId(),
                application.getUserId(),
                application.getTeacherName(),
                application.getIdCardMasked(),
                revealIdentity
                        ? crypto.decryptIdentity(
                                application.getIdCardCiphertext(),
                                application.getIdCardIv()
                        )
                        : null,
                application.getGender(),
                application.getEducationLevel(),
                application.getEducationBackground(),
                application.getInstitution(),
                application.getProvince(),
                application.getCity(),
                application.getDistrict(),
                application.getBio(),
                application.getTeachingContent(),
                tags(application.getTeachingTags()),
                application.getAvailability(),
                application.getHourlyRate(),
                application.getPriceDescription(),
                application.getContactWechat(),
                application.getContactQq(),
                application.getContactEmail(),
                application.getStatus(),
                application.getRejectionReason(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getUpdatedAt()
        );
    }

    /** 执行 applicationSummary 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private TeacherApplicationSummary applicationSummary(
            TeacherApplicationAdminView application
    ) {
        return new TeacherApplicationSummary(
                application.getId(),
                application.getUserId(),
                application.getUsername(),
                application.getNickname(),
                application.getTeacherName(),
                application.getIdCardMasked(),
                application.getProvince(),
                application.getCity(),
                application.getInstitution(),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getReviewedAt(),
                application.getUpdatedAt()
        );
    }

    /** 转换或规范化Tags数据，不引入额外持久化副作用。 */
    private List<String> normalizeTags(List<String> source) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        source.forEach(value -> {
            String normalized = clean(value);
            if (!normalized.isBlank()) tags.add(normalized);
        });
        if (tags.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少填写一个教授内容标签");
        }
        return List.copyOf(tags);
    }

    /** 执行 tags 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private List<String> tags(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw internal("教师标签数据格式错误");
        }
    }

    /** 执行 json 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw internal("教师信息序列化失败");
        }
    }

    /** 校验Contact及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void requireContact(String wechat, String qq, String email) {
        if (optional(wechat) == null && optional(qq) == null && optional(email) == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "微信、QQ、邮箱至少填写一项"
            );
        }
    }

    /** 校验d原因及相关业务前置条件，不满足时抛出明确业务异常。 */
    private String requiredReason(String value, String message) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return normalized;
    }

    /** 执行 clean 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /** 执行 optional 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 执行 internal 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException internal(String message) {
        return new BusinessException(ErrorCode.INTERNAL_ERROR, message);
    }
}
