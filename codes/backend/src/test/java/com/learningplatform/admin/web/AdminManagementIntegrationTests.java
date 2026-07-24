package com.learningplatform.admin.web;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/question-schema.sql", "/sql/exam-schema.sql"})
class AdminManagementIntegrationTests {
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

    private User admin;
    private User learner;
    private User publisher;
    private String adminToken;
    private String learnerToken;

    @BeforeEach
    void setUp() throws Exception {
        admin = createUser("stage_i_admin", RoleCode.ADMIN);
        learner = createUser("stage_i_learner", RoleCode.USER);
        publisher = createUser("stage_i_publisher", RoleCode.PUBLISHER);
        adminToken = login(admin.getUsername());
        learnerToken = login(learner.getUsername());
    }

    @Test
    void listsFiltersAndProtectsAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("keyword", "learner")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username")
                        .value("stage_i_learner"))
                .andExpect(jsonPath("$.data.items[0].passwordHash")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.items[0].lastLoginIp")
                        .doesNotExist());

        mockMvc.perform(get("/api/admin/users")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void disablesUserAndInvalidatesExistingTokenImmediately() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/status", learner.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/auth/me")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(put("/api/admin/users/{id}/status", learner.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(put("/api/admin/users/{id}/status", admin.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("不能禁用当前登录的管理员账号"));
    }

    @Test
    void replacesRolesAndAppliesAuthorizationWithoutNewToken() throws Exception {
        mockMvc.perform(get("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/users/{id}/roles", learner.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"USER\",\"PUBLISHER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(2));

        mockMvc.perform(get("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/users/{id}/roles", learner.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"USER\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/users/{id}/roles", admin.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"USER\"]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("不能移除当前登录账号的管理员角色"));
    }

    @Test
    void viewsExamsAcrossPublishersButOrdinaryUserCannot() throws Exception {
        long examId = insertExam();

        mockMvc.perform(get("/api/admin/exams")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("publisherId", publisher.getId().toString())
                        .param("keyword", "阶段I"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].exam.id").value(examId))
                .andExpect(jsonPath("$.data.items[0].publisherUsername")
                        .value(publisher.getUsername()));

        mockMvc.perform(get("/api/admin/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.management.exam.name")
                        .value("阶段I管理查看考试"))
                .andExpect(jsonPath("$.data.management.candidates.length()")
                        .value(1))
                .andExpect(jsonPath("$.data.publisherUsername")
                        .value(publisher.getUsername()));

        mockMvc.perform(get("/api/admin/exams")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void recordsAndProtectsCriticalOperationLogsWithoutSensitiveBodies()
            throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/roles", learner.getId())
                        .header(AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"USER\",\"PUBLISHER\"]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "stage_i_learner",
                                  "password": "WrongPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("module", "USER")
                        .param("action", "CHANGE_ROLES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].operatorId")
                        .value(admin.getId()))
                .andExpect(jsonPath("$.data.items[0].operatorName")
                        .value(admin.getUsername()))
                .andExpect(jsonPath("$.data.items[0].targetId")
                        .value(learner.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].result")
                        .value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].requestPath")
                        .value("/api/admin/users/"
                                + learner.getId() + "/roles"))
                .andExpect(jsonPath("$.data.items[0].detailJson")
                        .value("{\"httpStatus\":200}"))
                .andExpect(jsonPath("$.data.items[0].requestId")
                        .isNotEmpty());

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("module", "AUTH")
                        .param("result", "FAILURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].operatorName")
                        .value(learner.getUsername()))
                .andExpect(jsonPath("$.data.items[0].detailJson")
                        .value("{\"httpStatus\":401}"))
                .andExpect(jsonPath("$.data.items[0].detailJson")
                        .value(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "WrongPassword123"
                                )
                        )));

        mockMvc.perform(get("/api/admin/operation-logs")
                        .header(AUTHORIZATION, bearer(learnerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addsExplicitApiSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "X-Content-Type-Options",
                        "nosniff"
                ))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string(
                        "Referrer-Policy",
                        "strict-origin-when-cross-origin"
                ))
                .andExpect(header().string(
                        "Permissions-Policy",
                        "camera=(), microphone=(), geolocation=(), payment=()"
                ))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; "
                                + "base-uri 'none'; form-action 'none'"
                ));
    }

    @Test
    void keepsAuthenticationAndAiConfigurationSecretsOutOfResponses()
            throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "stage_i_admin",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.passwordHash")
                        .doesNotExist())
                .andReturn();
        String loginBody = loginResult.getResponse()
                .getContentAsString();
        org.assertj.core.api.Assertions.assertThat(loginBody)
                .doesNotContain(PASSWORD)
                .doesNotContain("passwordHash");

        MvcResult configResult = mockMvc.perform(
                        get("/api/admin/ai/config")
                                .header(
                                        AUTHORIZATION,
                                        bearer(adminToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyConfigured")
                        .isBoolean())
                .andExpect(jsonPath("$.data.apiKey").doesNotExist())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(
                configResult.getResponse().getContentAsString()
        ).doesNotContain("test-secret-key")
                .doesNotContain("test-access-key")
                .doesNotContain("test-only-secret");
    }

    private long insertExam() {
        jdbcTemplate.update("""
                INSERT INTO exam_paper (
                    creator_id, name, description, total_score,
                    question_count, status
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, publisher.getId(), "阶段I测试试卷", "管理查看",
                100, 0, "READY");
        Long paperId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM exam_paper",
                Long.class
        );
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        LocalDateTime endAt = startAt.plusHours(2);
        jdbcTemplate.update("""
                INSERT INTO exam (
                    publisher_id, paper_id, name, instructions,
                    start_at, end_at, duration_minutes, passing_score,
                    show_result_immediately, show_answer_after_finish,
                    status, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, publisher.getId(), paperId, "阶段I管理查看考试", "请按时参加",
                Timestamp.valueOf(startAt), Timestamp.valueOf(endAt),
                60, 60, false, true, "DRAFT", 0);
        Long examId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM exam",
                Long.class
        );
        jdbcTemplate.update("""
                INSERT INTO exam_candidate (exam_id, user_id, status)
                VALUES (?, ?, ?)
                """, examId, learner.getId(), "ASSIGNED");
        return examId;
    }

    private User createUser(String username, RoleCode roleCode) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE);
        userService.create(user);
        roleService.assignRole(user.getId(), roleCode, null);
        return user;
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("accessToken").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsByteArray()
        ).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
