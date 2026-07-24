package com.learningplatform.auth.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.auth.security.ApiAccessDeniedHandler;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/sql/user-schema.sql")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void createExistingUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setNickname("Alice");
        user.setEmail("alice@example.com");
        user.setStatus(UserStatus.ACTIVE);
        userService.create(user);
        roleService.assignRole(user.getId(), RoleCode.USER, null);
    }

    @Test
    void registersAndLogsInWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Bob_01",
                                  "password": "Password123",
                                  "nickname": "Bob",
                                  "email": "bob@example.com",
                                  "phone": "13800000000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("bob_01"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "Bob_01",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(7200))
                .andExpect(jsonPath("$.data.user.username").value("bob_01"));
    }

    @Test
    void rejectsDuplicateRegistrationAndWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "Password123",
                                  "nickname": "Another Alice"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900))
                .andExpect(jsonPath("$.message").value("用户名已被使用"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "alice",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void validatesRegistrationInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "a",
                                  "password": "123",
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.password").exists())
                .andExpect(jsonPath("$.data.nickname").exists());
    }

    @Test
    @WithMockUser(username = "alice")
    void readsAndUpdatesCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "Alice Updated",
                                  "email": "alice.new@example.com",
                                  "phone": "13900000000",
                                  "gender": "FEMALE",
                                  "bio": "Keep learning"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("Alice Updated"))
                .andExpect(jsonPath("$.data.email").value("alice.new@example.com"))
                .andExpect(jsonPath("$.data.phone").value("13900000000"))
                .andExpect(jsonPath("$.data.bio").value("Keep learning"));
    }

    @Test
    @WithMockUser(username = "alice")
    void logsOutAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void authenticatesBearerTokenAndRejectsMissingOrInvalidToken() throws Exception {
        String token = loginAndGetToken("alice", "Password123");

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void rejectsPreviouslyIssuedTokenAfterAccountIsDisabled() throws Exception {
        String token = loginAndGetToken("alice", "Password123");
        User alice = userService.findByUsername("alice").orElseThrow();
        userService.updateStatus(alice.getId(), UserStatus.DISABLED);

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void accessDeniedHandlerUsesUnifiedForbiddenResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.get("code").asInt()).isEqualTo(40300);
        assertThat(body.get("message").asText()).isEqualTo("无权执行此操作");
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data")
                .path("accessToken")
                .asText();
    }
}
