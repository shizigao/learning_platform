package com.learningplatform.ai.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.service.AiResultPersistenceService;
import com.learningplatform.ai.service.AiTaskLifecycleService;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql", "/sql/ai-schema.sql"})
class AiLearningControllerIntegrationTests {
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AiTaskLifecycleService taskService;
    @Autowired
    private AiResultPersistenceService persistenceService;

    private User user;
    private String token;
    private String otherToken;
    private String adminToken;
    private long freeContentId;
    private long paidContentId;

    @BeforeEach
    void setUp() throws Exception {
        user = createUser("ai_user");
        grantAiQuota(user.getId(), 20);
        createUser("ai_other");
        User publisher = createUser("ai_publisher");
        User admin = createUser("ai_admin");
        roleService.assignRole(admin.getId(), RoleCode.ADMIN, null);
        token = login("ai_user");
        otherToken = login("ai_other");
        adminToken = login("ai_admin");
        freeContentId = insertContent(
                user.getId(),
                "数据库事务基础",
                "事务用于保持数据一致性",
                "ACID 包括原子性、一致性、隔离性和持久性。",
                true
        );
        paidContentId = insertContent(
                publisher.getId(),
                "付费数据库资料",
                "需要购买后才能使用",
                "这是一段受保护的文本。",
                false
        );
    }

    @Test
    void generatesPersistsAndIdempotentlyReturnsStructuredSummary() throws Exception {
        MvcResult generated = mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/summaries",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"summary-request-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.task.provider").value("mock"))
                .andExpect(jsonPath("$.data.summary").value(
                        org.hamcrest.Matchers.containsString("模拟摘要")
                ))
                .andExpect(jsonPath("$.data.knowledgePoints.length()").value(3))
                .andExpect(jsonPath("$.data.reviewOutline").isNotEmpty())
                .andExpect(jsonPath("$.data.sourceVersion").isNotEmpty())
                .andReturn();
        JsonNode first = data(generated);
        long taskId = first.path("task").path("id").asLong();
        long summaryId = first.path("id").asLong();

        mockMvc.perform(get("/api/ai/tasks/{taskId}", taskId)
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskType").value("SUMMARY"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        mockMvc.perform(get(
                                "/api/ai/contents/{contentId}/summaries/latest",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(summaryId));
        mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/summaries",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"summary-request-1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(summaryId))
                .andExpect(jsonPath("$.data.task.id").value(taskId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_task WHERE request_id = 'summary-request-1'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_summary WHERE task_id = ?",
                Integer.class,
                taskId
        )).isEqualTo(1);
        assertThat(aiQuota(user.getId())).isEqualTo(19);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record WHERE task_id = ?",
                Integer.class,
                taskId
        )).isEqualTo(1);
        mockMvc.perform(get("/api/ai/tasks")
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"));
        mockMvc.perform(get("/api/ai/usage-records")
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(1))
                .andExpect(jsonPath("$.data[0].balanceBefore").value(20))
                .andExpect(jsonPath("$.data[0].balanceAfter").value(19));
    }

    @Test
    void createsOwnedConversationAndPersistsExplanationMessages() throws Exception {
        MvcResult created = mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/conversations",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(
                        org.hamcrest.Matchers.containsString("数据库事务基础")
                ))
                .andExpect(jsonPath("$.data.messages.length()").value(0))
                .andReturn();
        long conversationId = data(created).path("id").asLong();

        MvcResult explanation = mockMvc.perform(post(
                                "/api/ai/conversations/{conversationId}/messages",
                                conversationId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId":"explanation-request-1",
                                  "question":"请解释 ACID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.question.role").value("USER"))
                .andExpect(jsonPath("$.data.answer.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.answer.content").value(
                        org.hamcrest.Matchers.containsString("模拟 AI 讲解")
                ))
                .andReturn();
        long taskId = data(explanation).path("task").path("id").asLong();

        mockMvc.perform(get(
                                "/api/ai/conversations/{conversationId}",
                                conversationId
                        )
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"));
        mockMvc.perform(get(
                                "/api/ai/contents/{contentId}/conversations",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post(
                                "/api/ai/conversations/{conversationId}/messages",
                                conversationId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId":"explanation-request-1",
                                  "question":"请解释 ACID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.task.id").value(taskId));
        mockMvc.perform(get(
                                "/api/ai/conversations/{conversationId}",
                                conversationId
                        )
                        .header(AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_message WHERE conversation_id = ?",
                Integer.class,
                conversationId
        )).isEqualTo(2);
        assertThat(aiQuota(user.getId())).isEqualTo(19);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record WHERE task_id = ?",
                Integer.class,
                taskId
        )).isEqualTo(1);
    }

    @Test
    void rejectsAiAccessToUnpurchasedContentAndCrossUserTasks() throws Exception {
        mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/summaries",
                                paidContentId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("购买该资料后才能访问正文或文件"));

        MvcResult generated = mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/summaries",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"private-task"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long taskId = data(generated).path("task").path("id").asLong();

        mockMvc.perform(get("/api/ai/tasks/{taskId}", taskId)
                        .header(AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordsFailedTaskWithoutResultOrDeductionWhenQuotaIsUnavailable()
            throws Exception {
        User noQuotaUser = createUser("ai_no_quota");
        String noQuotaToken = login("ai_no_quota");

        mockMvc.perform(post(
                                "/api/ai/contents/{contentId}/summaries",
                                freeContentId
                        )
                        .header(AUTHORIZATION, bearer(noQuotaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"no-quota-task"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("AI 可用次数不足，请先购买 AI 次数包"));

        assertThat(aiQuota(noQuotaUser.getId())).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ai_task WHERE request_id = 'no-quota-task'",
                String.class
        )).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT error_code FROM ai_task WHERE request_id = 'no-quota-task'",
                String.class
        )).isEqualTo("AI_QUOTA_INSUFFICIENT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_summary",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record",
                Integer.class
        )).isZero();
    }

    @Test
    void rollsBackResultUsageAndQuotaWhenSuccessStateCannotBeCommitted() {
        AiTask pending = taskService.create(
                "atomic-rollback-task",
                user.getId(),
                freeContentId,
                null,
                AiTaskType.SUMMARY,
                10
        ).task();

        assertThatThrownBy(() -> persistenceService.saveSummary(
                pending,
                freeContentId,
                "摘要",
                "[\"知识点\"]",
                "提纲",
                "version"
        )).hasMessage("AI 任务状态已变化");

        assertThat(aiQuota(user.getId())).isEqualTo(20);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_summary WHERE task_id = ?",
                Integer.class,
                pending.getId()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record WHERE task_id = ?",
                Integer.class,
                pending.getId()
        )).isZero();
        assertThat(taskService.require(pending.getId(), user.getId()).getStatus())
                .isEqualTo(com.learningplatform.ai.domain.AiTaskStatus.PENDING);
    }

    @Test
    void adminReadsEffectiveAiConfigurationWithoutSecretMaterial()
            throws Exception {
        mockMvc.perform(get("/api/admin/ai/config")
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("mock"))
                .andExpect(jsonPath("$.data.model")
                        .value("mock-learning-assistant-test"))
                .andExpect(jsonPath("$.data.mockMode").value(true))
                .andExpect(jsonPath("$.data.mockScenario").value("success"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").value(false))
                .andExpect(jsonPath("$.data.thinkingEnabled").value(false))
                .andExpect(jsonPath("$.data.limits.maxInputChars").value(100000))
                .andExpect(jsonPath("$.data.limits.maxConcurrentPerUser").value(1));

        mockMvc.perform(get("/api/admin/ai/config")
                        .header(AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    private long insertContent(
            Long publisherId,
            String title,
            String summary,
            String articleBody,
            boolean free
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO learning_content (
                    publisher_id, category_id, title, summary, content_type,
                    article_body, is_free, price, status, published_at
                ) VALUES (?, 1, ?, ?, 'ARTICLE', ?, ?, ?, 'PUBLISHED', CURRENT_TIMESTAMP)
                """,
                publisherId,
                title,
                summary,
                articleBody,
                free,
                free ? "0.00" : "6.60"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM learning_content WHERE title = ?",
                Long.class,
                title
        );
    }

    private User createUser(String username) {
        User created = new User();
        created.setUsername(username);
        created.setPasswordHash(passwordEncoder.encode(PASSWORD));
        created.setNickname(username);
        created.setStatus(UserStatus.ACTIVE);
        userService.create(created);
        roleService.assignRole(created.getId(), RoleCode.USER, null);
        return created;
    }

    private void grantAiQuota(Long userId, int quantity) {
        jdbcTemplate.update(
                """
                INSERT INTO user_entitlement (
                    user_id, entitlement_type, total_quantity,
                    available_quantity, status, effective_at, version
                ) VALUES (?, 'AI_QUOTA', ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, 0)
                """,
                userId,
                quantity,
                quantity
        );
    }

    private int aiQuota(Long userId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(available_quantity), 0)
                FROM user_entitlement
                WHERE user_id = ?
                  AND entitlement_type = 'AI_QUOTA'
                  AND status = 'ACTIVE'
                """,
                Integer.class,
                userId
        );
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return data(result).path("accessToken").asText();
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsByteArray()
        ).path("data");
    }

    private String bearer(String value) {
        return "Bearer " + value;
    }
}
