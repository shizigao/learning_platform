package com.learningplatform.question.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.question.dto.CandidateQuestionResponse;
import com.learningplatform.question.service.QuestionService;
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

import static org.assertj.core.api.Assertions.assertThat;
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
@Sql({"/sql/user-schema.sql", "/sql/question-schema.sql"})
class QuestionControllerIntegrationTests {
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
    private QuestionService questionService;

    private String publisherToken;
    private String otherPublisherToken;
    private String userToken;
    private long bankId;

    @BeforeEach
    void setUp() throws Exception {
        createUser("question_publisher", RoleCode.PUBLISHER);
        createUser("other_question_publisher", RoleCode.PUBLISHER);
        createUser("question_user", RoleCode.USER);
        publisherToken = login("question_publisher");
        otherPublisherToken = login("other_question_publisher");
        userToken = login("question_user");
        bankId = createBank(publisherToken, "Java 基础题库");
    }

    @Test
    void createsAndListsAllFiveQuestionTypes() throws Exception {
        createQuestion(singleChoiceJson(bankId), "SINGLE_CHOICE");
        createQuestion(multipleChoiceJson(bankId), "MULTIPLE_CHOICE");
        MvcResult trueFalse = createQuestion(trueFalseJson(bankId), "TRUE_FALSE");
        createQuestion(fillBlankJson(bankId), "FILL_BLANK");
        createQuestion(shortAnswerJson(bankId), "SHORT_ANSWER");

        mockMvc.perform(get("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .param("bankId", String.valueOf(bankId))
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.items.length()").value(5));

        JsonNode trueFalseData = responseData(trueFalse);
        assertThat(trueFalseData.path("options")).hasSize(2);
        assertThat(trueFalseData.at("/answer/acceptedAnswers/0/0").asText()).isEqualTo("TRUE");
    }

    @Test
    void supportsQuestionUpdateDetailAndSoftDelete() throws Exception {
        long questionId = responseData(createQuestion(singleChoiceJson(bankId), "SINGLE_CHOICE"))
                .path("id").asLong();

        String updated = singleChoiceJson(bankId)
                .replace("Java 的入口方法是？", "Java 程序的入口方法是？")
                .replace("\"A\"]]", "\"B\"]]");
        mockMvc.perform(put("/api/publisher/questions/{id}", questionId)
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stem").value("Java 程序的入口方法是？"))
                .andExpect(jsonPath("$.data.answer.acceptedAnswers[0][0]").value("B"));

        mockMvc.perform(get("/api/publisher/questions/{id}", questionId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].isCorrect").doesNotExist());

        mockMvc.perform(delete("/api/publisher/questions/{id}", questionId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/publisher/questions/{id}", questionId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void preventsCrossPublisherAndOrdinaryUserManagement() throws Exception {
        long questionId = responseData(createQuestion(singleChoiceJson(bankId), "SINGLE_CHOICE"))
                .path("id").asLong();

        mockMvc.perform(get("/api/publisher/questions/{id}", questionId)
                        .header(AUTHORIZATION, bearer(otherPublisherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(post("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleChoiceJson(bankId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void rejectsAnswersThatDoNotMatchTheQuestionType() throws Exception {
        String invalid = singleChoiceJson(bankId).replace("\"A\"]]", "\"Z\"]]");
        mockMvc.perform(post("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("正确答案引用了不存在的选项：Z"));

        String invalidTrueFalse = trueFalseJson(bankId).replace("\"TRUE\"", "\"YES\"");
        mockMvc.perform(post("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidTrueFalse))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("判断题答案必须是TRUE或FALSE"));
    }

    @Test
    void candidateProjectionCannotSerializeAnswersAnalysisOrCorrectFlags() throws Exception {
        long questionId = responseData(createQuestion(singleChoiceJson(bankId), "SINGLE_CHOICE"))
                .path("id").asLong();
        CandidateQuestionResponse candidate = questionService.candidateProjection(questionId);
        JsonNode json = objectMapper.valueToTree(candidate);

        assertThat(json.has("answer")).isFalse();
        assertThat(json.has("analysis")).isFalse();
        assertThat(json.at("/options/0").has("correct")).isFalse();
        assertThat(json.at("/options/0").has("isCorrect")).isFalse();
        assertThat(json.path("stem").asText()).isEqualTo("Java 的入口方法是？");
    }

    @Test
    void refusesToDeleteNonEmptyQuestionBank() throws Exception {
        createQuestion(singleChoiceJson(bankId), "SINGLE_CHOICE");
        mockMvc.perform(delete("/api/publisher/question-banks/{id}", bankId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    private long createBank(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/publisher/question-banks")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "阶段E自动化测试题库",
                                  "status": "ACTIVE"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private MvcResult createQuestion(String json, String expectedType) throws Exception {
        return mockMvc.perform(post("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionType").value(expectedType))
                .andReturn();
    }

    private String singleChoiceJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "SINGLE_CHOICE",
                  "stem": "Java 的入口方法是？",
                  "options": [
                    {"key": "A", "text": "main", "sortOrder": 0},
                    {"key": "B", "text": "start", "sortOrder": 1}
                  ],
                  "answer": {"acceptedAnswers": [["A"]]},
                  "analysis": "main 是程序入口。",
                  "defaultScore": 2
                }
                """.formatted(targetBankId);
    }

    private String multipleChoiceJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "MULTIPLE_CHOICE",
                  "stem": "哪些是 Java 关键字？",
                  "options": [
                    {"key": "A", "text": "class"},
                    {"key": "B", "text": "public"},
                    {"key": "C", "text": "hello"}
                  ],
                  "answer": {"acceptedAnswers": [["A", "B"]]},
                  "analysis": "class 和 public 都是关键字。",
                  "defaultScore": 4
                }
                """.formatted(targetBankId);
    }

    private String trueFalseJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "TRUE_FALSE",
                  "stem": "Java 是跨平台语言。",
                  "options": [],
                  "answer": {"acceptedAnswers": [["TRUE"]]},
                  "analysis": "字节码运行于 JVM。",
                  "defaultScore": 1
                }
                """.formatted(targetBankId);
    }

    private String fillBlankJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "FILL_BLANK",
                  "stem": "Java 源文件扩展名是____，字节码扩展名是____。",
                  "options": [],
                  "answer": {"acceptedAnswers": [[".java", "java"], [".class", "class"]]},
                  "analysis": "源文件编译后生成字节码。",
                  "defaultScore": 4,
                  "fillBlankAutoGradable": true,
                  "caseSensitive": false
                }
                """.formatted(targetBankId);
    }

    private String shortAnswerJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "SHORT_ANSWER",
                  "stem": "请简述 JVM 的作用。",
                  "options": [],
                  "answer": {"acceptedAnswers": [["JVM 负责加载并执行 Java 字节码。"]]},
                  "analysis": "人工评分时结合要点判断。",
                  "defaultScore": 10
                }
                """.formatted(targetBankId);
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
