package com.learningplatform.exam.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;
import com.learningplatform.order.mapper.UserEntitlementMapper;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
@Sql({"/sql/user-schema.sql", "/sql/question-schema.sql", "/sql/exam-schema.sql"})
class ExamPublishingIntegrationTests {
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
    private UserEntitlementMapper entitlementMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.learningplatform.exam.service.ExamTimeoutScheduler timeoutScheduler;

    private User publisher;
    private User otherPublisher;
    private User candidate;
    private User outsider;
    private String publisherToken;
    private String otherPublisherToken;
    private String candidateToken;
    private String outsiderToken;
    private long bankId;
    private long singleQuestionId;
    private long shortQuestionId;
    private long paperId;

    @BeforeEach
    void setUp() throws Exception {
        publisher = createUser("exam_publisher", RoleCode.PUBLISHER);
        otherPublisher = createUser("other_exam_publisher", RoleCode.PUBLISHER);
        candidate = createUser("exam_candidate", RoleCode.USER);
        outsider = createUser("exam_outsider", RoleCode.USER);
        publisherToken = login(publisher.getUsername());
        otherPublisherToken = login(otherPublisher.getUsername());
        candidateToken = login(candidate.getUsername());
        outsiderToken = login(outsider.getUsername());

        bankId = createBank(publisherToken);
        singleQuestionId = createQuestion(publisherToken, singleChoiceJson(bankId));
        shortQuestionId = createQuestion(publisherToken, shortAnswerJson(bankId));
        paperId = createPaperWithQuestions(publisherToken, singleQuestionId, shortQuestionId);
    }

    @Test
    void buildsFixedPaperWithOrderedSnapshotsAndCalculatedTotal() throws Exception {
        mockMvc.perform(get("/api/publisher/papers/{id}", paperId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paper.status").value("READY"))
                .andExpect(jsonPath("$.data.paper.questionCount").value(2))
                .andExpect(jsonPath("$.data.paper.totalScore").value(100.0))
                .andExpect(jsonPath("$.data.questions[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data.questions[0].score").value(40.0))
                .andExpect(jsonPath("$.data.questions[0].answer.acceptedAnswers[0][0]").value("A"))
                .andExpect(jsonPath("$.data.questions[1].sortOrder").value(2));

        mockMvc.perform(put("/api/publisher/questions/{id}", singleQuestionId)
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleChoiceJson(bankId).replace("Java 的入口方法是？", "修改后的题干")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publisher/papers/{id}", paperId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].stem").value("Java 的入口方法是？"));
    }

    @Test
    void rejectsDuplicateOrderAndQuestionsOwnedByAnotherPublisher() throws Exception {
        long draftPaper = createPaper(publisherToken, "非法组卷测试");
        mockMvc.perform(put("/api/publisher/papers/{id}/questions", draftPaper)
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questions": [
                                    {"questionId": %d, "sortOrder": 1, "score": 10},
                                    {"questionId": %d, "sortOrder": 1, "score": 20}
                                  ]
                                }
                                """.formatted(singleQuestionId, shortQuestionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));

        long otherBank = createBank(otherPublisherToken);
        long otherQuestion = createQuestion(otherPublisherToken, singleChoiceJson(otherBank));
        mockMvc.perform(put("/api/publisher/papers/{id}/questions", draftPaper)
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questions": [
                                    {"questionId": %d, "sortOrder": 1, "score": 10}
                                  ]
                                }
                                """.formatted(otherQuestion)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void createsExamWithCandidatesAndValidatesScheduleRules() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);
        MvcResult created = createExam(publisherToken, paperId, start, end, 90, 60, candidate.getId());
        long examId = responseData(created).path("exam").path("id").asLong();

        mockMvc.perform(get("/api/publisher/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.exam.showResultImmediately").value(true))
                .andExpect(jsonPath("$.data.exam.showAnswerAfterFinish").value(false))
                .andExpect(jsonPath("$.data.candidates[0].userId").value(candidate.getId()))
                .andExpect(jsonPath("$.data.candidates[0].username").value("exam_candidate"));

        mockMvc.perform(post("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(paperId, start, end, 121, 60, candidate.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("答题时长不能超过考试开放时长"));

        mockMvc.perform(post("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(paperId, start, end, 90, 101, candidate.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("及格分不能超过试卷总分"));
    }

    @Test
    void publisherCanSearchActiveExamCandidates() throws Exception {
        mockMvc.perform(get("/api/publisher/exam-candidates")
                        .param("keyword", "exam_candidate")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(candidate.getId()))
                .andExpect(jsonPath("$.data[0].username").value("exam_candidate"));

        mockMvc.perform(get("/api/publisher/exam-candidates")
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedCandidateCannotLoadQuestionsBeforeExamStarts() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                45,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);

        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("考试尚未开始"));
    }

    @Test
    void assignedCandidateCanReadInstructionsAndCheckEligibilityBeforeStart() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(2),
                60,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/exams/{id}/overview", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.instructions").value("请独立完成"))
                .andExpect(jsonPath("$.data.paper.questionCount").value(2))
                .andExpect(jsonPath("$.data.eligibility.eligible").value(true))
                .andExpect(jsonPath("$.data.eligibility.canStart").value(false))
                .andExpect(jsonPath("$.data.eligibility.reason").value("考试尚未开始"))
                .andReturn();
        assertThat(responseData(result).toString())
                .doesNotContain("questions", "acceptedAnswers", "\"answer\"", "\"analysis\"");

        mockMvc.perform(get("/api/exams/{id}/eligibility", examId)
                        .header(AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("你不是本场考试的指定考生"));
    }

    @Test
    void startsExamIdempotentlyAndUsesServerOwnedPersonalDeadline() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());

        MvcResult first = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.remainingSeconds").value(1800))
                .andReturn();
        JsonNode firstData = responseData(first);
        long attemptId = firstData.path("attemptId").asLong();
        LocalDateTime startedAt = LocalDateTime.parse(firstData.path("startedAt").asText());
        LocalDateTime deadlineAt = LocalDateTime.parse(firstData.path("deadlineAt").asText());
        assertThat(Duration.between(startedAt, deadlineAt)).isEqualTo(Duration.ofMinutes(30));
        assertThat(firstData.toString())
                .doesNotContain("acceptedAnswers", "\"answer\"", "\"analysis\"");

        mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(attemptId))
                .andExpect(jsonPath("$.data.startedAt").value(firstData.path("startedAt").asText()))
                .andExpect(jsonPath("$.data.deadlineAt").value(firstData.path("deadlineAt").asText()));

        mockMvc.perform(get("/api/publisher/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates[0].status").value("STARTED"))
                .andExpect(jsonPath("$.data.candidates[0].startedAt").isNotEmpty());
    }

    @Test
    void clampsLateCandidateDeadlineToGlobalExamEnd() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(30);
        LocalDateTime end = start.plusMinutes(40);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                end,
                40,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = responseData(result);
        assertThat(LocalDateTime.parse(data.path("deadlineAt").asText()))
                .isEqualTo(LocalDateTime.parse(data.path("exam").path("endAt").asText()));
        assertThat(data.path("remainingSeconds").asLong()).isBetween(590L, 600L);
    }

    @Test
    void savesSingleAndBatchAnswersIdempotentlyAndRestoresThem() throws Exception {
        long examId = createPublishedActiveExam();
        MvcResult started = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answers.length()").value(2))
                .andExpect(jsonPath("$.data.answers[0].gradingStatus").value("UNANSWERED"))
                .andReturn();
        long attemptId = responseData(started).path("attemptId").asLong();

        String singlePayload = """
                {"values": ["A"], "text": null}
                """;
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, singleQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singlePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.values[0]").value("A"))
                .andExpect(jsonPath("$.data.gradingStatus").value("SAVED"));
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, singleQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singlePayload))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/exams/{examId}/answers", examId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {
                                      "questionId": %d,
                                      "answer": {"values": ["A"], "text": null}
                                    },
                                    {
                                      "questionId": %d,
                                      "answer": {
                                        "values": [],
                                        "text": "JVM 负责加载并执行 Java 字节码。"
                                      }
                                    }
                                  ]
                                }
                                """.formatted(singleQuestionId, shortQuestionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/exams/{id}/session", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(attemptId))
                .andExpect(jsonPath("$.data.answers[0].values[0]").value("A"))
                .andExpect(jsonPath("$.data.answers[1].text")
                        .value("JVM 负责加载并执行 Java 字节码。"));

        Integer answerRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_answer WHERE attempt_id = ?",
                Integer.class,
                attemptId
        );
        assertThat(answerRows).isEqualTo(2);
    }

    @Test
    void serializesConcurrentSavesWithoutCreatingDuplicateAnswers() throws Exception {
        long examId = createPublishedActiveExam();
        MvcResult started = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        long attemptId = responseData(started).path("attemptId").asLong();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = List.of("A", "B").stream()
                    .map(value -> executor.submit(() -> {
                        ready.countDown();
                        go.await();
                        return mockMvc.perform(put(
                                                "/api/exams/{examId}/answers/{questionId}",
                                                examId,
                                                singleQuestionId
                                        )
                                        .header(AUTHORIZATION, bearer(candidateToken))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"values\":[\"" + value + "\"]}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    }))
                    .toList();
            ready.await();
            go.countDown();
            assertThat(results.get(0).get()).isEqualTo(200);
            assertThat(results.get(1).get()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM exam_answer WHERE attempt_id = ? AND question_id = ?",
                Integer.class,
                attemptId,
                singleQuestionId
        );
        String answerJson = jdbcTemplate.queryForObject(
                "SELECT answer_json FROM exam_answer WHERE attempt_id = ? AND question_id = ?",
                String.class,
                attemptId,
                singleQuestionId
        );
        assertThat(rows).isEqualTo(1);
        assertThat(answerJson).isIn("{\"values\":[\"A\"]}", "{\"values\":[\"B\"]}");
    }

    @Test
    void manualSubmissionIsIdempotentAndPreventsFurtherAnswerChanges() throws Exception {
        long examId = createPublishedActiveExam();
        mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, singleQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[\"A\"],\"text\":null}"))
                .andExpect(status().isOk());

        MvcResult submitted = mockMvc.perform(post("/api/exams/{id}/submit", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.submissionType").value("MANUAL"))
                .andExpect(jsonPath("$.data.answeredCount").value(1))
                .andExpect(jsonPath("$.data.totalQuestions").value(2))
                .andReturn();
        long attemptId = responseData(submitted).path("attemptId").asLong();

        mockMvc.perform(post("/api/exams/{id}/submit", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value(attemptId))
                .andExpect(jsonPath("$.data.submissionType").value("MANUAL"));
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, shortQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[],\"text\":\"提交后修改\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("试卷已提交，不能继续修改答案"));
    }

    @Test
    void timeoutScannerReliablySubmitsExpiredAttemptFromMysql() throws Exception {
        long examId = createPublishedActiveExam();
        MvcResult started = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        long attemptId = responseData(started).path("attemptId").asLong();
        jdbcTemplate.update(
                "UPDATE exam_attempt SET deadline_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)),
                attemptId
        );

        timeoutScheduler.scan();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT status, submission_type FROM exam_attempt WHERE id = ?",
                attemptId
        );
        assertThat(row.get("status")).isEqualTo("COMPLETED");
        assertThat(row.get("submission_type")).isEqualTo("TIMEOUT");
        mockMvc.perform(get("/api/publisher/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates[0].status").value("SUBMITTED"));
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, singleQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[\"A\"],\"text\":null}"))
                .andExpect(status().isConflict());
    }

    @Test
    void gradesObjectiveAnswersAndCompletesManualReviewWithValidatedScores() throws Exception {
        long examId = createPublishedActiveExam();
        jdbcTemplate.update(
                "UPDATE exam SET show_result_immediately = FALSE WHERE id = ?",
                examId
        );
        MvcResult started = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        long attemptId = responseData(started).path("attemptId").asLong();

        mockMvc.perform(put("/api/exams/{examId}/answers", examId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionId": %d, "answer": {"values": ["A"]}},
                                    {"questionId": %d, "answer": {
                                      "values": [],
                                      "text": "JVM 加载并执行字节码"
                                    }}
                                  ]
                                }
                                """.formatted(singleQuestionId, shortQuestionId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/exams/{id}/submit", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADING"));

        mockMvc.perform(get("/api/exams/{id}/result", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("主观题批改完成后方可查看成绩"));
        mockMvc.perform(get("/api/publisher/exams/{examId}/grading/attempts", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].pendingReviewCount").value(1))
                .andExpect(jsonPath("$.data[0].totalScore").value(40.0));
        mockMvc.perform(get(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}",
                                examId,
                                attemptId
                        )
                        .header(AUTHORIZATION, bearer(otherPublisherToken)))
                .andExpect(status().isForbidden());

        MvcResult detail = mockMvc.perform(get(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}",
                                examId,
                                attemptId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].score").value(40.0))
                .andExpect(jsonPath("$.data.questions[0].gradingStatus").value("AUTO_GRADED"))
                .andExpect(jsonPath("$.data.questions[1].gradingStatus").value("PENDING_REVIEW"))
                .andReturn();
        long answerId = responseData(detail).path("questions").get(1).path("answerId").asLong();

        mockMvc.perform(put(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}/answers/{answerId}",
                                examId,
                                attemptId,
                                answerId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":61,\"comment\":\"超过满分\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("得分不能超过本题满分"));
        mockMvc.perform(put(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}/answers/{answerId}",
                                examId,
                                attemptId,
                                answerId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":50,\"comment\":\"要点基本完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(50.0))
                .andExpect(jsonPath("$.data.gradingStatus").value("GRADED"));
        mockMvc.perform(post(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}/complete",
                                examId,
                                attemptId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(90.0))
                .andExpect(jsonPath("$.data.passed").value(true))
                .andExpect(jsonPath("$.data.gradingCompleted").value(true));

        mockMvc.perform(get("/api/exams/{id}/result", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.correctCount").value(1))
                .andExpect(jsonPath("$.data.result.incorrectCount").value(1))
                .andExpect(jsonPath("$.data.result.unansweredCount").value(0))
                .andExpect(jsonPath("$.data.answersVisible").value(false))
                .andExpect(jsonPath("$.data.questions[0].correctAnswer").doesNotExist());
    }

    @Test
    void gradesConfiguredFillBlanksAndRevealsAnswersOnlyAfterExamFinishes() throws Exception {
        long autoFillId = createQuestion(publisherToken, fillBlankJson(bankId, true));
        long reviewFillId = createQuestion(publisherToken, fillBlankJson(bankId, false));
        long fillPaperId = createPaperWithQuestions(
                publisherToken,
                autoFillId,
                reviewFillId
        );
        long examId = createPublishedActiveExam(fillPaperId);
        MvcResult started = mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        long attemptId = responseData(started).path("attemptId").asLong();

        mockMvc.perform(put("/api/exams/{examId}/answers", examId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers": [
                                    {"questionId": %d, "answer": {
                                      "values": ["java", "错误答案"]
                                    }},
                                    {"questionId": %d, "answer": {
                                      "values": ["人工复核"]
                                    }}
                                  ]
                                }
                                """.formatted(autoFillId, reviewFillId)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/exams/{id}/submit", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("GRADING"));

        mockMvc.perform(get("/api/exams/{id}/result", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.totalScore").value(20.0))
                .andExpect(jsonPath("$.data.result.gradingCompleted").value(false))
                .andExpect(jsonPath("$.data.answersVisible").value(false));

        MvcResult detail = mockMvc.perform(get(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}",
                                examId,
                                attemptId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].score").value(20.0))
                .andExpect(jsonPath("$.data.questions[0].correct").value(false))
                .andExpect(jsonPath("$.data.questions[1].gradingStatus").value("PENDING_REVIEW"))
                .andReturn();
        long reviewAnswerId = responseData(detail).path("questions").get(1).path("answerId").asLong();
        mockMvc.perform(put(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}/answers/{answerId}",
                                examId,
                                attemptId,
                                reviewAnswerId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":40,\"comment\":\"复核正确\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(
                                "/api/publisher/exams/{examId}/grading/attempts/{attemptId}/complete",
                                examId,
                                attemptId
                        )
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalScore").value(60.0))
                .andExpect(jsonPath("$.data.passed").value(true));

        jdbcTemplate.update(
                "UPDATE exam SET end_at = ?, show_answer_after_finish = TRUE WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)),
                examId
        );
        mockMvc.perform(get("/api/exams/{id}/result", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answersVisible").value(true))
                .andExpect(jsonPath("$.data.questions[0].correctAnswer.acceptedAnswers[0][0]")
                        .value("Java"))
                .andExpect(jsonPath("$.data.questions[0].analysis").value("填空题解析。"));
    }

    @Test
    void calculatesAttendanceScoresPassRateAndQuestionCorrectRate() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId(),
                outsider.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/exams/{id}/start", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/exams/{examId}/answers/{questionId}", examId, singleQuestionId)
                        .header(AUTHORIZATION, bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"values\":[\"A\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/exams/{id}/submit", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/publisher/exams/{examId}/grading/statistics", examId)
                        .header(AUTHORIZATION, bearer(otherPublisherToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/publisher/exams/{examId}/grading/statistics", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCandidates").value(2))
                .andExpect(jsonPath("$.data.participatedCount").value(1))
                .andExpect(jsonPath("$.data.submittedCount").value(1))
                .andExpect(jsonPath("$.data.notParticipatedCount").value(1))
                .andExpect(jsonPath("$.data.gradedCount").value(1))
                .andExpect(jsonPath("$.data.averageScore").value(40.0))
                .andExpect(jsonPath("$.data.highestScore").value(40.0))
                .andExpect(jsonPath("$.data.lowestScore").value(40.0))
                .andExpect(jsonPath("$.data.passedCount").value(0))
                .andExpect(jsonPath("$.data.passRate").value(0.0))
                .andExpect(jsonPath("$.data.questions[0].gradedCount").value(1))
                .andExpect(jsonPath("$.data.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.data.questions[0].correctCount").value(1))
                .andExpect(jsonPath("$.data.questions[0].correctRate").value(100.0))
                .andExpect(jsonPath("$.data.questions[1].answeredCount").value(0))
                .andExpect(jsonPath("$.data.questions[1].correctRate").value(0.0));
    }

    @Test
    void consumesQuotaOnceAndMakesRepeatedPublishIdempotent() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                45,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();

        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("考试发布额度不足"));

        grantExamQuota(publisher.getId(), 2);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/publisher/exams/quota")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/publisher/exams/quota")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void supportsPublishedExamCancellationWithoutRefundingConsumedQuota() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusMinutes(20);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);

        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/publisher/exams/{id}/cancel", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.status").value("CANCELLED"));
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/publisher/exams/quota")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void candidateResponseHidesAnswersAndRejectsUnassignedUsers() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andReturn();
        JsonNode data = responseData(result);
        assertThat(data.toString()).doesNotContain("acceptedAnswers", "\"answer\"", "\"analysis\"");
        assertThat(data.at("/questions/0/options/0").has("correct")).isFalse();
        assertThat(data.at("/questions/0/options/0").has("isCorrect")).isFalse();

        mockMvc.perform(get("/api/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(outsiderToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void blocksCrossPublisherExamManagementAndPublishingWithoutCandidates() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusMinutes(10);
        long examId = responseData(createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();

        mockMvc.perform(get("/api/publisher/exams/{id}", examId)
                        .header(AUTHORIZATION, bearer(otherPublisherToken)))
                .andExpect(status().isForbidden());

        MvcResult noCandidates = createExam(
                publisherToken,
                paperId,
                start,
                start.plusHours(1),
                30,
                60
        );
        long noCandidateExamId = responseData(noCandidates).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", noCandidateExamId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("考试至少需要指定一名考生"));
        mockMvc.perform(get("/api/publisher/exams/quota")
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
    }

    private long createBank(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/publisher/question-banks")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "阶段E题库",
                                  "description": "试卷测试题库"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private long createPublishedActiveExam() throws Exception {
        return createPublishedActiveExam(paperId);
    }

    private long createPublishedActiveExam(long targetPaperId) throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        long examId = responseData(createExam(
                publisherToken,
                targetPaperId,
                start,
                start.plusHours(1),
                30,
                60,
                candidate.getId()
        )).path("exam").path("id").asLong();
        grantExamQuota(publisher.getId(), 1);
        mockMvc.perform(post("/api/publisher/exams/{id}/publish", examId)
                        .header(AUTHORIZATION, bearer(publisherToken)))
                .andExpect(status().isOk());
        return examId;
    }

    private long createQuestion(String token, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/publisher/questions")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private long createPaperWithQuestions(String token, long firstQuestionId, long secondQuestionId)
            throws Exception {
        long id = createPaper(token, "Java 固定试卷");
        mockMvc.perform(put("/api/publisher/papers/{id}/questions", id)
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "questions": [
                                    {"questionId": %d, "sortOrder": 1, "score": 40},
                                    {"questionId": %d, "sortOrder": 2, "score": 60}
                                  ]
                                }
                                """.formatted(firstQuestionId, secondQuestionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paper.totalScore").value(100.0));
        return id;
    }

    private long createPaper(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/publisher/papers")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "固定试卷自动化测试"
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("paper").path("id").asLong();
    }

    private MvcResult createExam(
            String token,
            long targetPaperId,
            LocalDateTime start,
            LocalDateTime end,
            int duration,
            int passingScore,
            Long... candidateIds
    ) throws Exception {
        return mockMvc.perform(post("/api/publisher/exams")
                        .header(AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(
                                targetPaperId,
                                start,
                                end,
                                duration,
                                passingScore,
                                candidateIds
                        )))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String examJson(
            long targetPaperId,
            LocalDateTime start,
            LocalDateTime end,
            int duration,
            int passingScore,
            Long... candidateIds
    ) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "paperId", targetPaperId,
                "name", "Java 阶段测试",
                "instructions", "请独立完成",
                "startAt", start,
                "endAt", end,
                "durationMinutes", duration,
                "passingScore", passingScore,
                "showResultImmediately", true,
                "showAnswerAfterFinish", false,
                "candidateUserIds", java.util.List.of(candidateIds)
        ));
    }

    private String singleChoiceJson(long targetBankId) {
        return """
                {
                  "bankId": %d,
                  "questionType": "SINGLE_CHOICE",
                  "stem": "Java 的入口方法是？",
                  "options": [
                    {"key": "A", "text": "main"},
                    {"key": "B", "text": "start"}
                  ],
                  "answer": {"acceptedAnswers": [["A"]]},
                  "analysis": "main 是入口。",
                  "defaultScore": 2
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
                  "answer": {"acceptedAnswers": [["加载并执行 Java 字节码。"]]},
                  "analysis": "人工评分。",
                  "defaultScore": 10
                }
                """.formatted(targetBankId);
    }

    private String fillBlankJson(long targetBankId, boolean autoGradable) {
        String acceptedAnswers = autoGradable
                ? "[[\"Java\"],[\"Virtual Machine\",\"JVM\"]]"
                : "[[\"人工复核\"]]";
        return """
                {
                  "bankId": %d,
                  "questionType": "FILL_BLANK",
                  "stem": "请填写测试答案。",
                  "options": [],
                  "answer": {"acceptedAnswers": %s},
                  "analysis": "填空题解析。",
                  "defaultScore": 10,
                  "fillBlankAutoGradable": %s,
                  "caseSensitive": false
                }
                """.formatted(targetBankId, acceptedAnswers, autoGradable);
    }

    private void grantExamQuota(Long userId, int quantity) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(userId);
        entitlement.setEntitlementType(EntitlementType.EXAM_QUOTA);
        entitlement.setTotalQuantity(quantity);
        entitlement.setAvailableQuantity(quantity);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setEffectiveAt(LocalDateTime.now().minusMinutes(1));
        entitlement.setVersion(0);
        assertThat(entitlementMapper.insert(entitlement)).isEqualTo(1);
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
