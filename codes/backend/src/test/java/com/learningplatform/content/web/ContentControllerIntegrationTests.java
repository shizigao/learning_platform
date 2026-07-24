package com.learningplatform.content.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.content.service.ContentCategoryService;
import com.learningplatform.content.service.ContentAccessService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql"})
class ContentControllerIntegrationTests {
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
    private ContentCategoryService categoryService;

    @Autowired
    private ContentAccessService accessService;

    private String userToken;
    private String publisherToken;
    private String otherPublisherToken;
    private String adminToken;
    private Long categoryId;
    private User contentUser;

    @BeforeEach
    void setUp() throws Exception {
        contentUser = createUser("content_user", RoleCode.USER);
        createUser("content_publisher", RoleCode.PUBLISHER);
        createUser("other_publisher", RoleCode.PUBLISHER);
        createUser("content_admin", RoleCode.ADMIN);
        userToken = login("content_user");
        publisherToken = login("content_publisher");
        otherPublisherToken = login("other_publisher");
        adminToken = login("content_admin");
        categoryId = categoryService.listEnabled().get(0).id();
    }

    @Test
    void completesPublisherReviewAndPublicQueryWorkflow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/publisher/contents")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long contentId = responseData(createResult).path("id").asLong();

        mockMvc.perform(get("/api/publisher/contents")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(contentId));

        mockMvc.perform(get("/api/publisher/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(otherPublisherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(post("/api/publisher/contents/{id}/submit", contentId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mockMvc.perform(post("/api/admin/contents/{id}/approve", contentId)
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .param("keyword", "Spring")
                        .param("contentType", "ARTICLE")
                        .param("free", "true")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].title").value("Spring Boot 入门"))
                .andExpect(jsonPath("$.data.items[0].publisherName").value("content_publisher"));

        mockMvc.perform(get("/api/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleBody").value("这是完整正文"))
                .andExpect(jsonPath("$.data.publisherName").value("content_publisher"));
    }

    @Test
    void blocksOrdinaryUserFromPublisherAndAdminOperations() throws Exception {
        mockMvc.perform(post("/api/publisher/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(post("/api/admin/categories")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "越权分类",
                                  "slug": "forbidden-category",
                                  "sortOrder": 30,
                                  "enabled": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void rejectsCrossPublisherEditAtHttpBoundary() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/publisher/contents")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(contentJson()))
                .andExpect(status().isOk())
                .andReturn();
        long contentId = responseData(createResult).path("id").asLong();

        String unauthorizedUpdate = objectMapper.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "被其他发布者篡改的标题",
                "summary", "不应保存",
                "contentType", "ARTICLE",
                "articleBody", "不应保存的正文",
                "isFree", true,
                "price", 0
        ));
        mockMvc.perform(put("/api/publisher/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(otherPublisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unauthorizedUpdate))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/publisher/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot 入门"))
                .andExpect(jsonPath("$.data.articleBody").value("这是完整正文"));
    }

    @Test
    void validatesPagingParametersAtHttpBoundary() throws Exception {
        mockMvc.perform(get("/api/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .param("pageNumber", "0")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void enforcesPaidAccessAndExposesLearningInteractionApis() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/publisher/contents")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paidContentJson()))
                .andExpect(status().isOk())
                .andReturn();
        long contentId = responseData(createResult).path("id").asLong();

        mockMvc.perform(post("/api/publisher/contents/{id}/submit", contentId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/contents/{id}/approve", contentId)
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasAccess").value(false))
                .andExpect(jsonPath("$.data.articleBody").doesNotExist());
        mockMvc.perform(post("/api/learning/contents/{id}/start", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        accessService.grantContentAccess(contentUser.getId(), contentId, null, null);

        mockMvc.perform(get("/api/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasAccess").value(true))
                .andExpect(jsonPath("$.data.articleBody").value("受保护的完整正文"));
        mockMvc.perform(post("/api/learning/contents/{id}/start", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercent").value(0.0));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/learning/contents/{id}/progress", contentId)
                        .header(AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "progressPercent": 45.50,
                                  "lastPosition": "page:5"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercent").value(45.5));

        mockMvc.perform(post("/api/contents/{id}/like", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true));
        mockMvc.perform(post("/api/contents/{id}/like", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
        mockMvc.perform(post("/api/contents/{id}/favorite", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.favorited").value(true));
        mockMvc.perform(post("/api/contents/{id}/favorite", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
        mockMvc.perform(get("/api/learning/favorites")
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(contentId));
        mockMvc.perform(post("/api/contents/{id}/comments", contentId)
                        .header(AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "这份资料很有帮助"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("这份资料很有帮助"));
        mockMvc.perform(get("/api/contents/{id}/comments", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void parameterizesInjectionLikeSearchInput() throws Exception {
        MvcResult createResult = mockMvc.perform(
                        post("/api/publisher/contents")
                                .header(
                                        AUTHORIZATION,
                                        bearer(publisherToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentJson())
                )
                .andExpect(status().isOk())
                .andReturn();
        long contentId = responseData(createResult).path("id").asLong();
        mockMvc.perform(post(
                        "/api/publisher/contents/{id}/submit",
                        contentId
                ).header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/admin/contents/{id}/approve",
                        contentId
                ).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .param("keyword", "%' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id")
                        .value(contentId));
    }

    @Test
    void takesContentOfflineAndRevokesAllUserFacingAccess()
            throws Exception {
        MvcResult createResult = mockMvc.perform(
                        post("/api/publisher/contents")
                                .header(
                                        AUTHORIZATION,
                                        bearer(publisherToken)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(contentJson())
                )
                .andExpect(status().isOk())
                .andReturn();
        long contentId = responseData(createResult).path("id").asLong();
        mockMvc.perform(post(
                        "/api/publisher/contents/{id}/submit",
                        contentId
                ).header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/admin/contents/{id}/approve",
                        contentId
                ).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                        "/api/admin/contents/{id}/offline",
                        contentId
                ).header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        mockMvc.perform(get("/api/contents/{id}", contentId)
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
        mockMvc.perform(post(
                        "/api/learning/contents/{id}/start",
                        contentId
                ).header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/contents")
                        .header(AUTHORIZATION, bearer(userToken))
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    private String contentJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "Spring Boot 入门",
                "summary", "适合初学者的课程",
                "contentType", "ARTICLE",
                "articleBody", "这是完整正文",
                "isFree", true,
                "price", 0
        ));
    }

    private String paidContentJson() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "categoryId", categoryId,
                "title", "付费 Spring 进阶",
                "summary", "需要权益才能学习",
                "contentType", "ARTICLE",
                "articleBody", "受保护的完整正文",
                "isFree", false,
                "price", 29.90
        ));
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
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
