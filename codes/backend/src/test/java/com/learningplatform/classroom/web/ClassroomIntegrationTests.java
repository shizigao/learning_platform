package com.learningplatform.classroom.web;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/content-schema.sql"})
class ClassroomIntegrationTests {
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

    private User owner;
    private User manager;
    private User member;
    private String ownerToken;
    private String managerToken;
    private String memberToken;

    @BeforeEach
    void setUp() throws Exception {
        owner = createUser("class_owner", RoleCode.PUBLISHER);
        manager = createUser("class_manager", RoleCode.PUBLISHER);
        member = createUser("class_member", RoleCode.USER);
        ownerToken = login(owner.getUsername());
        managerToken = login(manager.getUsername());
        memberToken = login(member.getUsername());
    }

    @Test
    void createsJoinsAndManagesClassWithRoleBoundaries() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/class-management/classes")
                        .header(AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "数据库学习班",
                                  "description": "用于班级功能联调"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRole").value("OWNER"))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.inviteCode").isNotEmpty())
                .andReturn();

        JsonNode created = responseData(createResult);
        long classId = created.path("id").asLong();
        String inviteCode = created.path("inviteCode").asText();

        mockMvc.perform(get("/api/class-management/classes")
                        .header(AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        join(memberToken, inviteCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentRole").value("MEMBER"))
                .andExpect(jsonPath("$.data.inviteCode").doesNotExist());
        join(managerToken, inviteCode).andExpect(status().isOk());

        mockMvc.perform(get("/api/classes/{classId}/members", classId)
                        .header(AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].role").value("OWNER"));

        mockMvc.perform(post("/api/classes/{classId}/announcements", classId)
                        .header(AUTHORIZATION, bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(
                                "/api/class-management/classes/{classId}/members/{userId}/role",
                                classId,
                                manager.getId()
                        )
                        .header(AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/classes/{classId}/announcements", classId)
                        .header(AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcementJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("第一次班级公告"))
                .andExpect(jsonPath("$.data.authorName").value(manager.getNickname()));

        mockMvc.perform(delete(
                                "/api/class-management/classes/{classId}/members/{userId}",
                                classId,
                                member.getId()
                        )
                        .header(AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/classes/{classId}", classId)
                        .header(AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isForbidden());

        join(memberToken, inviteCode)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你已被移出该班级，请联系班级拥有者"));
    }

    private org.springframework.test.web.servlet.ResultActions join(
            String token,
            String inviteCode
    ) throws Exception {
        return mockMvc.perform(post("/api/classes/join")
                .header(AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteCode\":\"" + inviteCode + "\"}"));
    }

    private String announcementJson() {
        return """
                {
                  "title": "第一次班级公告",
                  "body": "请按时完成 **学习任务**。",
                  "pinned": true
                }
                """;
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
