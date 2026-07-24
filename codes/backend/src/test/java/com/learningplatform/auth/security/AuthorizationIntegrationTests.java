package com.learningplatform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.LearningPlatformApplication;
import com.learningplatform.common.api.ApiResponse;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = LearningPlatformApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/sql/user-schema.sql")
@Import(AuthorizationIntegrationTests.AuthorizationTestController.class)
class AuthorizationIntegrationTests {
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

    private User user;
    private User otherUser;
    private User publisher;
    private User admin;

    @BeforeEach
    void createUsers() {
        user = createUser("user_a", RoleCode.USER);
        otherUser = createUser("user_b", RoleCode.USER);
        publisher = createUser("publisher_a", RoleCode.PUBLISHER);
        admin = createUser("admin_a", RoleCode.ADMIN);
    }

    @Test
    void enforcesRoleNamespaces() throws Exception {
        String userToken = login("user_a");
        String publisherToken = login("publisher_a");
        String adminToken = login("admin_a");

        getWithToken("/api/user/test", userToken)
                .andExpect(status().isOk());
        getWithToken("/api/publisher/test", userToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        getWithToken("/api/admin/test", userToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权执行此操作"));

        getWithToken("/api/publisher/test", publisherToken)
                .andExpect(status().isOk());
        getWithToken("/api/admin/test", publisherToken)
                .andExpect(status().isForbidden());

        getWithToken("/api/publisher/test", adminToken)
                .andExpect(status().isOk());
        getWithToken("/api/admin/test", adminToken)
                .andExpect(status().isOk());
    }

    @Test
    void enforcesResourceOwnershipAndAllowsAdminOverride() throws Exception {
        String userToken = login("user_a");
        String otherUserToken = login("user_b");
        String adminToken = login("admin_a");
        String ownedUrl = "/api/owned/" + user.getId();

        getWithToken(ownedUrl, userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(user.getId()));
        getWithToken(ownedUrl, otherUserToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        getWithToken(ownedUrl, adminToken)
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousRoleAccess() throws Exception {
        mockMvc.perform(get("/api/publisher/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    private User createUser(String username, RoleCode roleCode) {
        User created = new User();
        created.setUsername(username);
        created.setPasswordHash(passwordEncoder.encode(PASSWORD));
        created.setNickname(username);
        created.setStatus(UserStatus.ACTIVE);
        userService.create(created);
        roleService.assignRole(created.getId(), roleCode, null);
        return created;
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
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private org.springframework.test.web.servlet.ResultActions getWithToken(
            String path,
            String token
    ) throws Exception {
        return mockMvc.perform(get(path).header(AUTHORIZATION, "Bearer " + token));
    }

    @RestController
    static class AuthorizationTestController {

        @GetMapping("/api/user/test")
        ApiResponse<String> user(Authentication authentication) {
            return ApiResponse.success(authentication.getName());
        }

        @GetMapping("/api/publisher/test")
        ApiResponse<String> publisher(Authentication authentication) {
            return ApiResponse.success(authentication.getName());
        }

        @GetMapping("/api/admin/test")
        ApiResponse<String> admin(Authentication authentication) {
            return ApiResponse.success(authentication.getName());
        }

        @OwnerOrAdmin
        @GetMapping("/api/owned/{ownerId}")
        ApiResponse<Map<String, Long>> owned(@PathVariable Long ownerId) {
            return ApiResponse.success(Map.of("ownerId", ownerId));
        }
    }
}
