package com.learningplatform.learning.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.ContentWriteRequest;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.content.service.ContentCategoryService;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.learning.dto.CreateCommentRequest;
import com.learningplatform.learning.dto.UpdateLearningProgressRequest;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql"})
class LearningAccessInteractionServiceTests {
    @Autowired
    private LearningContentService contentService;

    @Autowired
    private ContentAccessService accessService;

    @Autowired
    private ContentCategoryService categoryService;

    @Autowired
    private LearningProgressService progressService;

    @Autowired
    private ContentInteractionService interactionService;

    @Autowired
    private UserService userService;

    private User publisher;
    private User learner;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        publisher = createUser("access_publisher");
        learner = createUser("access_learner");
        categoryId = categoryService.listEnabled().get(0).id();
    }

    @Test
    void protectsPaidBodyAndEnablesLearningAfterEntitlementGrant() {
        Long paidContentId = publish("付费深度课程", false, "付费课程正文");

        ContentDetailResponse locked = contentService.publishedDetail(
                paidContentId,
                learner.getId(),
                false
        );
        assertThat(locked.hasAccess()).isFalse();
        assertThat(locked.articleBody()).isNull();
        assertThatThrownBy(() -> progressService.start(learner.getId(), false, paidContentId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        accessService.grantContentAccess(learner.getId(), paidContentId, null, null);
        ContentDetailResponse unlocked = contentService.publishedDetail(
                paidContentId,
                learner.getId(),
                false
        );
        assertThat(unlocked.hasAccess()).isTrue();
        assertThat(unlocked.articleBody()).isEqualTo("付费课程正文");

        assertThat(progressService.start(learner.getId(), false, paidContentId).progressPercent())
                .isEqualByComparingTo("0.00");
        assertThat(progressService.update(
                learner.getId(),
                false,
                paidContentId,
                new UpdateLearningProgressRequest(new BigDecimal("60.00"), "page:6")
        ).progressPercent()).isEqualByComparingTo("60.00");
        assertThat(progressService.update(
                learner.getId(),
                false,
                paidContentId,
                new UpdateLearningProgressRequest(new BigDecimal("20.00"), "page:2")
        ).progressPercent()).isEqualByComparingTo("20.00");
        var completed = progressService.update(
                learner.getId(),
                false,
                paidContentId,
                new UpdateLearningProgressRequest(new BigDecimal("100.00"), "completed")
        );
        assertThat(completed.completedAt()).isNotNull();
        var reopened = progressService.update(
                learner.getId(),
                false,
                paidContentId,
                new UpdateLearningProgressRequest(new BigDecimal("80.00"), "reviewing")
        );
        assertThat(reopened.progressPercent()).isEqualByComparingTo("80.00");
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    void ignoresExpiredContentEntitlement() {
        Long paidContentId = publish("已过期权益课程", false, "受保护正文");
        accessService.grantContentAccess(
                learner.getId(),
                paidContentId,
                null,
                LocalDateTime.now().minusMinutes(1)
        );

        assertThat(contentService.publishedDetail(paidContentId, learner.getId(), false).hasAccess())
                .isFalse();
    }

    @Test
    void enforcesLikeFavoriteAndCommentConstraints() {
        Long freeContentId = publish("互动课程", true, "免费正文");
        Long otherContentId = publish("另一课程", true, "另一正文");

        assertThat(interactionService.like(freeContentId, learner.getId(), false).liked()).isTrue();
        assertThatThrownBy(() -> interactionService.like(freeContentId, learner.getId(), false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(interactionService.unlike(freeContentId, learner.getId(), false).liked()).isFalse();
        assertThatThrownBy(() -> interactionService.unlike(freeContentId, learner.getId(), false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        assertThat(interactionService.favorite(freeContentId, learner.getId(), false).favorited()).isTrue();
        assertThatThrownBy(() -> interactionService.favorite(freeContentId, learner.getId(), false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertThat(interactionService.unfavorite(freeContentId, learner.getId(), false).favorited()).isFalse();

        var root = interactionService.comment(
                freeContentId,
                learner.getId(),
                false,
                new CreateCommentRequest(null, "讲解很清晰")
        );
        interactionService.comment(
                freeContentId,
                publisher.getId(),
                false,
                new CreateCommentRequest(root.id(), "感谢反馈")
        );
        assertThat(interactionService.comments(
                freeContentId,
                learner.getId(),
                false,
                new PageQuery()
        ).total()).isEqualTo(2);

        assertThatThrownBy(() -> interactionService.comment(
                otherContentId,
                learner.getId(),
                false,
                new CreateCommentRequest(root.id(), "错误的跨资料回复")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    private Long publish(String title, boolean free, String body) {
        ContentDetailResponse draft = contentService.create(
                publisher.getId(),
                new ContentWriteRequest(
                        categoryId,
                        title,
                        title + "简介",
                        ContentType.ARTICLE,
                        body,
                        free,
                        free ? BigDecimal.ZERO : new BigDecimal("19.90")
                )
        );
        contentService.submit(draft.id(), publisher.getId());
        contentService.approve(draft.id());
        return draft.id();
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
