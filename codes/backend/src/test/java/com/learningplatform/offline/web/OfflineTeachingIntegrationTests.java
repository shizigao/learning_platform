package com.learningplatform.offline.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import com.learningplatform.offline.mapper.OfflineTeachingMapper;
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

import java.math.BigDecimal;

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
@Sql({"/sql/user-schema.sql", "/sql/offline-teaching-schema.sql"})
class OfflineTeachingIntegrationTests {
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
    private OfflineTeachingMapper offlineTeachingMapper;

    private User publisher;
    private String publisherToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        publisher = createUser("offline_publisher", RoleCode.PUBLISHER);
        User admin = createUser("offline_admin", RoleCode.ADMIN);
        User user = createUser("offline_user", RoleCode.USER);
        publisherToken = login(publisher.getUsername());
        adminToken = login(admin.getUsername());
        userToken = login(user.getUsername());
    }

    @Test
    void appliesReviewsAndPublishesTeacherWithRoleBoundaries() throws Exception {
        mockMvc.perform(get("/api/offline-teaching/application")
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

        MvcResult saved = mockMvc.perform(put("/api/offline-teaching/application")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.idCardMasked").value("1101**********002X"))
                .andExpect(jsonPath("$.data.idCardNumber").doesNotExist())
                .andReturn();
        long applicationId = responseData(saved).path("id").asLong();

        mockMvc.perform(post("/api/offline-teaching/application/submit")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(put("/api/offline-teaching/application")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/offline-teachers/applications")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].teacherName").value("王老师"));

        mockMvc.perform(get(
                                "/api/admin/offline-teachers/applications/{id}",
                                applicationId
                        )
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.idCardNumber")
                        .value("11010519491231002X"));

        mockMvc.perform(post(
                                "/api/admin/offline-teachers/applications/{id}/approve",
                                applicationId
                        )
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/offline-teaching/teachers")
                        .header(AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].userId")
                        .value(publisher.getId()))
                .andExpect(jsonPath("$.data.items[0].contactWechat")
                        .value("teacher_wechat"))
                .andExpect(jsonPath("$.data.items[0].availability")
                        .value("工作日19:00-21:00，周末09:00-18:00"));

        assertThat(offlineTeachingMapper.findRecommendationCandidates(
                "广东省",
                "广州市",
                new BigDecimal("80.00")
        )).hasSize(1);
    }

    private String applicationJson() {
        return """
                {
                  "teacherName": "王老师",
                  "idCardNumber": "11010519491231002X",
                  "gender": "MALE",
                  "educationLevel": "MASTER",
                  "educationBackground": "计算机科学硕士，具有多年教学经验",
                  "institution": "示例教育机构",
                  "province": "广东省",
                  "city": "广州市",
                  "district": "天河区",
                  "bio": "耐心讲解并根据学生情况制定学习计划",
                  "teachingContent": "数据库、Java与后端开发",
                  "teachingTags": ["数据库", "Java"],
                  "availability": "工作日19:00-21:00，周末09:00-18:00",
                  "hourlyRate": 120,
                  "priceDescription": "具体价格可协商",
                  "contactWechat": "teacher_wechat",
                  "contactQq": "",
                  "contactEmail": ""
                }
                """;
    }

    private User createUser(String username, RoleCode role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE);
        userService.create(user);
        roleService.assignRole(user.getId(), role, null);
        return user;
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("accessToken").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
