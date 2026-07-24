package com.learningplatform.ai.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.ai.mock.scenario=failure")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql", "/sql/ai-schema.sql"})
class MockAiFailureIntegrationTests {
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

    private User user;
    private String token;
    private long contentId;

    @BeforeEach
    void setUp() throws Exception {
        user = createUser("mock_failure_user");
        grantQuota(user.getId(), 5);
        contentId = insertContent(user.getId(), "Mock 失败测试资料");
        token = login();
    }

    @Test
    void providerFailureCreatesSafeFailedTaskWithoutDeduction() throws Exception {
        mockMvc.perform(post("/api/ai/contents/{contentId}/summaries", contentId)
                        .header(AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requestId":"mock-provider-failure"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("AI 总结生成失败，请稍后重试"))
                .andExpect(content().string(not(containsString("模拟 AI 失败场景"))));

        assertThat(quota()).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ai_task WHERE request_id = 'mock-provider-failure'",
                String.class
        )).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT error_code FROM ai_task WHERE request_id = 'mock-provider-failure'",
                String.class
        )).isEqualTo("PROVIDER_ERROR");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_summary",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_usage_record",
                Integer.class
        )).isZero();
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

    private void grantQuota(Long userId, int quantity) {
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

    private long insertContent(Long publisherId, String title) {
        jdbcTemplate.update(
                """
                INSERT INTO learning_content (
                    publisher_id, category_id, title, summary, content_type,
                    article_body, is_free, price, status, published_at
                ) VALUES (?, 1, ?, '测试简介', 'ARTICLE',
                          '测试正文', TRUE, 0.00, 'PUBLISHED', CURRENT_TIMESTAMP)
                """,
                publisherId,
                title
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM learning_content WHERE title = ?",
                Long.class,
                title
        );
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"mock_failure_user","password":"Password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private int quota() {
        return jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(available_quantity), 0)
                FROM user_entitlement
                WHERE user_id = ? AND entitlement_type = 'AI_QUOTA'
                """,
                Integer.class,
                user.getId()
        );
    }
}
