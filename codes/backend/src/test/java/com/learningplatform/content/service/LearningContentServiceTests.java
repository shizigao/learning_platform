package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.ContentFileStatus;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.dto.CategoryWriteRequest;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.ContentListQuery;
import com.learningplatform.content.dto.ContentWriteRequest;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql"})
@Transactional
class LearningContentServiceTests {
    @Autowired
    private LearningContentService contentService;

    @Autowired
    private ContentCategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private ContentFileMapper fileMapper;

    private User publisher;
    private User otherPublisher;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        publisher = createUser("publisher_one");
        otherPublisher = createUser("publisher_two");
        categoryId = categoryService.listEnabled().get(0).id();
    }

    @Test
    void createsSubmitsApprovesSearchesAndViewsArticle() {
        ContentDetailResponse draft = contentService.create(
                publisher.getId(),
                request("Spring 安全入门", ContentType.ARTICLE, true, "完整正文")
        );
        assertThat(draft.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(contentService.listPublished(new ContentListQuery()).total()).isZero();

        assertThat(contentService.submit(draft.id(), publisher.getId()).status())
                .isEqualTo(ContentStatus.PENDING_REVIEW);
        assertThat(contentService.approve(draft.id()).status())
                .isEqualTo(ContentStatus.PUBLISHED);

        ContentListQuery query = new ContentListQuery();
        query.setKeyword("Spring");
        query.setContentType(ContentType.ARTICLE);
        query.setFree(true);
        assertThat(contentService.listPublished(query).total()).isEqualTo(1);

        ContentDetailResponse detail = contentService.publishedDetail(draft.id());
        assertThat(detail.articleBody()).isEqualTo("完整正文");
        assertThat(detail.viewCount()).isEqualTo(1);
    }

    @Test
    void supportsRejectEditOfflineAndRepublishTransitions() {
        ContentDetailResponse draft = contentService.create(
                publisher.getId(),
                request("付费文章", ContentType.ARTICLE, false, "付费正文")
        );
        contentService.submit(draft.id(), publisher.getId());
        ContentDetailResponse rejected = contentService.reject(draft.id(), "请完善简介");
        assertThat(rejected.status()).isEqualTo(ContentStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("请完善简介");

        ContentDetailResponse edited = contentService.update(
                draft.id(),
                publisher.getId(),
                false,
                request("付费文章（已完善）", ContentType.ARTICLE, false, "付费正文")
        );
        assertThat(edited.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(edited.rejectionReason()).isNull();

        contentService.submit(draft.id(), publisher.getId());
        contentService.approve(draft.id());
        assertThat(contentService.takeOffline(draft.id()).status()).isEqualTo(ContentStatus.OFFLINE);
        assertThat(contentService.republish(draft.id()).status()).isEqualTo(ContentStatus.PUBLISHED);

        ContentDetailResponse publicDetail = contentService.publishedDetail(draft.id());
        assertThat(publicDetail.articleBody()).isNull();
    }

    @Test
    void blocksCrossPublisherManagementAndIncompleteDocumentSubmission() {
        ContentDetailResponse article = contentService.create(
                publisher.getId(),
                request("归属测试", ContentType.ARTICLE, true, "正文")
        );
        assertThatThrownBy(() -> contentService.publisherDetail(
                article.id(),
                otherPublisher.getId(),
                false
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        ContentDetailResponse document = contentService.create(
                publisher.getId(),
                request("缺少文件", ContentType.DOCUMENT, true, null)
        );
        assertThatThrownBy(() -> contentService.submit(document.id(), publisher.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void validatesPaidPriceAndIllegalStateTransition() {
        assertThatThrownBy(() -> contentService.create(
                publisher.getId(),
                new ContentWriteRequest(
                        categoryId,
                        "价格错误",
                        null,
                        ContentType.ARTICLE,
                        "正文",
                        false,
                        BigDecimal.ZERO
                )
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        ContentDetailResponse draft = contentService.create(
                publisher.getId(),
                request("状态测试", ContentType.ARTICLE, true, "正文")
        );
        assertThatThrownBy(() -> contentService.approve(draft.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void blocksEditingWhilePendingReviewAndAllowsItAfterRejection() {
        ContentDetailResponse draft = contentService.create(
                publisher.getId(),
                request("待审核资料", ContentType.ARTICLE, true, "初始正文")
        );
        contentService.submit(draft.id(), publisher.getId());

        assertThatThrownBy(() -> contentService.update(
                draft.id(),
                publisher.getId(),
                false,
                request("审核中被修改", ContentType.ARTICLE, true, "修改正文")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        contentService.reject(draft.id(), "请修改内容");
        ContentDetailResponse edited = contentService.update(
                draft.id(),
                publisher.getId(),
                false,
                request("驳回后可修改", ContentType.ARTICLE, true, "修改后的正文")
        );
        assertThat(edited.status()).isEqualTo(ContentStatus.DRAFT);
        assertThat(edited.title()).isEqualTo("驳回后可修改");
    }

    @Test
    void persistsContentFilesAndAllowsCompleteDocumentSubmission() {
        ContentDetailResponse document = contentService.create(
                publisher.getId(),
                request("完整文档", ContentType.DOCUMENT, true, null)
        );
        ContentFile file = new ContentFile();
        file.setContentId(document.id());
        file.setFileRole(ContentFileRole.CONTENT);
        file.setOriginalName("guide.pdf");
        file.setObjectName("content/" + publisher.getId()
                + "/2026/07/123e4567-e89b-42d3-a456-426614174000.pdf");
        file.setBucketName("learning-platform-test");
        file.setMimeType("application/pdf");
        file.setExtension("pdf");
        file.setSizeBytes(1024L);
        file.setSortOrder(0);
        file.setStatus(ContentFileStatus.ACTIVE);
        file.setUploadedBy(publisher.getId());

        assertThat(fileMapper.insert(file)).isEqualTo(1);
        assertThat(fileMapper.countByContentIdAndRole(document.id(), ContentFileRole.CONTENT))
                .isEqualTo(1);
        assertThat(contentService.submit(document.id(), publisher.getId()).status())
                .isEqualTo(ContentStatus.PENDING_REVIEW);
    }

    @Test
    void createsUpdatesAndProtectsCategorySlugs() {
        var created = categoryService.create(new CategoryWriteRequest(
                null,
                "软件工程",
                "software-engineering",
                "软件工程资料",
                30,
                true
        ));
        var updated = categoryService.update(created.id(), new CategoryWriteRequest(
                null,
                "软件工程与实践",
                "software-engineering",
                "更新后的描述",
                31,
                true
        ));
        assertThat(updated.name()).isEqualTo("软件工程与实践");

        assertThatThrownBy(() -> categoryService.create(new CategoryWriteRequest(
                null,
                "重复分类",
                "software-engineering",
                null,
                40,
                true
        ))).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    private ContentWriteRequest request(
            String title,
            ContentType type,
            boolean free,
            String body
    ) {
        return new ContentWriteRequest(
                categoryId,
                title,
                title + "简介",
                type,
                body,
                free,
                free ? BigDecimal.ZERO : new BigDecimal("9.90")
        );
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$test-only-password-hash");
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE);
        return userService.create(user);
    }
}
