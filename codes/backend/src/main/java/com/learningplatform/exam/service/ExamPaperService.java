package com.learningplatform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
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

@Service
public class ExamPaperService {
    private static final BigDecimal MAX_TOTAL_SCORE = new BigDecimal("999999.99");
    private static final TypeReference<List<QuestionOptionResponse>> OPTION_LIST_TYPE =
            new TypeReference<>() {
            };

    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    public ExamPaperService(
            ExamPaperMapper paperMapper,
            ExamPaperQuestionMapper paperQuestionMapper,
            QuestionService questionService,
            ObjectMapper objectMapper
    ) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionService = questionService;
        this.objectMapper = objectMapper;
    }

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
        return detail(paperId, requesterId, requesterAdmin);
    }

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

    public List<CandidatePaperQuestionResponse> candidateQuestions(Long paperId) {
        getRequired(paperId);
        return paperQuestionMapper.findByPaperId(paperId).stream()
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
                .toList();
    }

    @Transactional
    public void delete(Long paperId, Long requesterId, boolean requesterAdmin) {
        ExamPaper paper = getRequired(paperId);
        assertOwnerOrAdmin(paper, requesterId, requesterAdmin);
        assertMutable(paper);
        if (paperMapper.softDelete(paperId) != 1) {
            throw invalidState("当前试卷不能删除");
        }
    }

    public ExamPaper getRequired(Long paperId) {
        return paperMapper.findById(paperId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在"));
    }

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

    public void assertOwnerOrAdmin(ExamPaper paper, Long requesterId, boolean requesterAdmin) {
        if (!requesterAdmin && !paper.getCreatorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的试卷");
        }
    }

    private void assertMutable(ExamPaper paper) {
        if (paper.getStatus() == ExamPaperStatus.ARCHIVED) {
            throw invalidState("已归档试卷不能修改");
        }
        if (paperMapper.countExamReferences(paper.getId()) > 0) {
            throw invalidState("试卷已被考试使用，不能继续修改");
        }
    }

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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "试卷快照序列化失败");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
