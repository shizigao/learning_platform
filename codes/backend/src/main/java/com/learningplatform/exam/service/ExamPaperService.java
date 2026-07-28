/* 文件职责：实现考试试卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.common.redis.RedisJsonCache;
import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamPaperQuestion;
import com.learningplatform.exam.domain.ExamPaperStatus;
import com.learningplatform.exam.dto.CandidatePaperQuestionResponse;
import com.learningplatform.exam.dto.ExamPaperDetailResponse;
import com.learningplatform.exam.dto.ExamPaperListQuery;
import com.learningplatform.exam.dto.ExamPaperSummaryResponse;
import com.learningplatform.exam.dto.ExamPaperWriteRequest;
import com.learningplatform.exam.dto.PaperQuestionManagementResponse;
import com.learningplatform.exam.dto.PaperQuestionWriteRequest;
import com.learningplatform.exam.dto.ReplacePaperQuestionsRequest;
import com.learningplatform.exam.mapper.ExamPaperMapper;
import com.learningplatform.exam.mapper.ExamPaperQuestionMapper;
import com.learningplatform.question.dto.QuestionAnswer;
import com.learningplatform.question.dto.QuestionManagementResponse;
import com.learningplatform.question.dto.QuestionOptionResponse;
import com.learningplatform.question.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;

@Service
/**
 * 实现考试试卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamPaperService {
    private static final BigDecimal MAX_TOTAL_SCORE = new BigDecimal("999999.99");
    /** 定义 OPTION_LIST_TYPE 常量，统一该组件使用的固定规则或默认值。 */
    private static final TypeReference<List<QuestionOptionResponse>> OPTION_LIST_TYPE =
            new TypeReference<>() {
            };
    private static final TypeReference<List<CandidatePaperQuestionResponse>>
            CANDIDATE_QUESTION_LIST_TYPE = new TypeReference<>() {
            };

    /** 访问试卷持久化数据。 */
    private final ExamPaperMapper paperMapper;
    /** 访问试卷题目持久化数据。 */
    private final ExamPaperQuestionMapper paperQuestionMapper;
    /** 委托题目执行对应领域规则。 */
    private final QuestionService questionService;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;
    private final RedisJsonCache redisJsonCache;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamPaperService(
            ExamPaperMapper paperMapper,
            ExamPaperQuestionMapper paperQuestionMapper,
            QuestionService questionService,
            ObjectMapper objectMapper,
            RedisJsonCache redisJsonCache
    ) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionService = questionService;
        this.objectMapper = objectMapper;
        this.redisJsonCache = redisJsonCache;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<ExamPaperSummaryResponse> list(Long creatorId, ExamPaperListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = paperMapper.countByCreator(creatorId, query.getStatus(), keyword);
        List<ExamPaperSummaryResponse> items = paperMapper.findByCreator(
                        creatorId,
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ExamPaperSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public ExamPaperDetailResponse create(Long creatorId, ExamPaperWriteRequest request) {
        ExamPaper paper = new ExamPaper();
        paper.setCreatorId(creatorId);
        paper.setName(request.name().trim());
        paper.setDescription(normalize(request.description()));
        if (paperMapper.insert(paper) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建试卷失败");
        }
        return detail(paper.getId(), creatorId, false);
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public ExamPaperDetailResponse update(
            Long paperId,
            Long requesterId,
            boolean requesterAdmin,
            ExamPaperWriteRequest request
    ) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        assertMutable(paper);
        paper.setName(request.name().trim());
        paper.setDescription(normalize(request.description()));
        if (paperMapper.update(paper) != 1) {
            throw invalidState("当前试卷不能修改");
        }
        return detail(paperId, requesterId, requesterAdmin);
    }

    @Transactional
    /** 更新Questions，通过返回值或版本条件识别并发状态变化。 */
    public ExamPaperDetailResponse replaceQuestions(
            Long paperId,
            Long requesterId,
            boolean requesterAdmin,
            ReplacePaperQuestionsRequest request
    ) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        assertMutable(paper);
        validateQuestionOrder(request.questions());

        List<ExamPaperQuestion> snapshots = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        for (PaperQuestionWriteRequest item : request.questions()) {
            QuestionManagementResponse source = questionService.detail(
                    item.questionId(),
                    requesterId,
                    requesterAdmin
            );
            ExamPaperQuestion snapshot = new ExamPaperQuestion();
            snapshot.setPaperId(paperId);
            snapshot.setQuestionId(source.id());
            snapshot.setSortOrder(item.sortOrder());
            snapshot.setScore(item.score());
            snapshot.setQuestionTypeSnapshot(source.questionType());
            snapshot.setStemSnapshot(source.stem());
            snapshot.setOptionsSnapshot(writeJson(source.options()));
            snapshot.setAnswerSnapshot(writeJson(java.util.Map.of(
                    "acceptedAnswers", source.answer().acceptedAnswers(),
                    "fillBlankAutoGradable", source.fillBlankAutoGradable(),
                    "caseSensitive", source.caseSensitive()
            )));
            snapshot.setAnalysisSnapshot(source.analysis());
            snapshots.add(snapshot);
            totalScore = totalScore.add(item.score());
        }
        if (totalScore.compareTo(MAX_TOTAL_SCORE) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷总分不能超过999999.99");
        }

        paperQuestionMapper.deleteByPaperId(paperId);
        for (ExamPaperQuestion snapshot : snapshots) {
            if (paperQuestionMapper.insert(snapshot) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存试卷题目失败");
            }
        }
        if (paperMapper.updateStatistics(
                paperId,
                totalScore,
                snapshots.size(),
                ExamPaperStatus.READY
        ) != 1) {
            throw invalidState("当前试卷不能重新组卷");
        }
        redisJsonCache.evictAfterCommit(candidateQuestionCacheKey(paperId));
        return detail(paperId, requesterId, requesterAdmin);
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public ExamPaperDetailResponse detail(
            Long paperId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        List<PaperQuestionManagementResponse> questions = paperQuestionMapper.findByPaperId(paperId)
                .stream()
                .map(this::managementQuestion)
                .toList();
        return new ExamPaperDetailResponse(ExamPaperSummaryResponse.from(paper), questions);
    }

    /** 判断是否满足didateQuestions条件，不修改持久化状态。 */
    public List<CandidatePaperQuestionResponse> candidateQuestions(Long paperId) {
        getRequired(paperId);
        return redisJsonCache.get(
                candidateQuestionCacheKey(paperId),
                CANDIDATE_QUESTION_LIST_TYPE,
                Duration.ofMinutes(30),
                () -> paperQuestionMapper.findByPaperId(paperId).stream()
                        .map(question -> new CandidatePaperQuestionResponse(
                                question.getId(),
                                question.getQuestionId(),
                                question.getSortOrder(),
                                question.getScore(),
                                question.getQuestionTypeSnapshot(),
                                question.getStemSnapshot(),
                                readOptions(question.getOptionsSnapshot()),
                                question.getQuestionTypeSnapshot() == com.learningplatform.question.domain.QuestionType.FILL_BLANK
                                        ? readAnswer(question.getAnswerSnapshot()).acceptedAnswers().size()
                                        : 0
                        ))
                        .toList()
        );
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long paperId, Long requesterId, boolean requesterAdmin) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        assertMutable(paper);
        if (paperMapper.softDelete(paperId) != 1) {
            throw invalidState("当前试卷不能删除");
        }
        redisJsonCache.evictAfterCommit(candidateQuestionCacheKey(paperId));
    }

    /** 返回Required。 */
    public ExamPaper getRequired(Long paperId) {
        return paperMapper.findById(paperId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在"));
    }

    /** 返回ReadyOwned。 */
    public ExamPaper getReadyOwned(Long paperId, Long requesterId, boolean requesterAdmin) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        if (paper.getStatus() != ExamPaperStatus.READY
                || paper.getQuestionCount() == null
                || paper.getQuestionCount() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷尚未完成组卷");
        }
        return paper;
    }

    /** 校验OwnerOr管理及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void assertOwnerOrAdmin(ExamPaper paper, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !paper.getCreatorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的试卷");
        }
    }

    /** 校验Mutable及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertMutable(ExamPaper paper) {
        if (paper.getStatus() == ExamPaperStatus.ARCHIVED) {
            throw invalidState("已归档试卷不能修改");
        }
        if (paperMapper.countExamReferences(paper.getId()) > 0) {
            throw invalidState("试卷已被考试使用，不能继续修改");
        }
    }

    /** 校验题目订单及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateQuestionOrder(List<PaperQuestionWriteRequest> questions) {
        Set<Long> questionIds = new HashSet<>();
        Set<Integer> sortOrders = new HashSet<>();
        for (PaperQuestionWriteRequest item : questions) {
            if (!questionIds.add(item.questionId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "同一道题不能重复加入试卷");
            }
            if (!sortOrders.add(item.sortOrder())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷题目顺序不能重复");
            }
        }
        for (int expected = 1; expected <= questions.size(); expected++) {
            if (!sortOrders.contains(expected)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "试卷题目顺序必须从1开始且连续");
            }
        }
    }

    /** 执行 managementQuestion 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private PaperQuestionManagementResponse managementQuestion(ExamPaperQuestion question) {
        return new PaperQuestionManagementResponse(
                question.getId(),
                question.getQuestionId(),
                question.getSortOrder(),
                question.getScore(),
                question.getQuestionTypeSnapshot(),
                question.getStemSnapshot(),
                readOptions(question.getOptionsSnapshot()),
                readAnswer(question.getAnswerSnapshot()),
                question.getAnalysisSnapshot()
        );
    }

    /** 查询Options相关数据；只返回当前调用方有权查看的结果。 */
    private List<QuestionOptionResponse> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, OPTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷选项快照格式损坏");
        }
    }

    /** 查询答案相关数据；只返回当前调用方有权查看的结果。 */
    private QuestionAnswer readAnswer(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            return new QuestionAnswer(objectMapper.convertValue(
                    root.path("acceptedAnswers"),
                    new TypeReference<List<List<String>>>() {
                    }
            ));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷答案快照格式损坏");
        }
    }

    /** 执行 writeJson 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷快照序列化失败");
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 invalidState 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }

    private String candidateQuestionCacheKey(Long paperId) {
        return "lp:v1:exam:paper:candidate:" + paperId;
    }
}
