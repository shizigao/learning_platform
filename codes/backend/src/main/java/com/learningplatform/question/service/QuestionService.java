/* 文件职责：实现题目业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：题库、题目、选项与标准答案；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.question.domain.Question;
import com.learningplatform.question.domain.QuestionBank;
import com.learningplatform.question.domain.QuestionOption;
import com.learningplatform.question.domain.QuestionStatus;
import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.CandidateQuestionResponse;
import com.learningplatform.question.dto.QuestionAnswer;
import com.learningplatform.question.dto.QuestionListQuery;
import com.learningplatform.question.dto.QuestionManagementResponse;
import com.learningplatform.question.dto.QuestionOptionResponse;
import com.learningplatform.question.dto.QuestionOptionWriteRequest;
import com.learningplatform.question.dto.QuestionWriteRequest;
import com.learningplatform.question.mapper.QuestionMapper;
import com.learningplatform.question.mapper.QuestionOptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
/**
 * 实现题目业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class QuestionService {
    /** 定义 MIN_CHOICE_OPTIONS 常量，统一该组件使用的固定规则或默认值。 */
    private static final int MIN_CHOICE_OPTIONS = 2;

    /** 访问题目持久化数据。 */
    private final QuestionMapper questionMapper;
    /** 访问选项持久化数据。 */
    private final QuestionOptionMapper optionMapper;
    /** 委托题库执行对应领域规则。 */
    private final QuestionBankService bankService;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public QuestionService(
            QuestionMapper questionMapper,
            QuestionOptionMapper optionMapper,
            QuestionBankService bankService,
            ObjectMapper objectMapper
    ) {
        this.questionMapper = questionMapper;
        this.optionMapper = optionMapper;
        this.bankService = bankService;
        this.objectMapper = objectMapper;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<QuestionManagementResponse> list(
            Long ownerId,
            QuestionListQuery query
    ) {
        if (query.getBankId() != null) {
            QuestionBank bank = bankService.getRequired(query.getBankId());
            bankService.assertOwnerOrAdmin(bank, ownerId, false);
        }
        String keyword = normalize(query.getKeyword());
        long total = questionMapper.countByOwner(
                ownerId,
                query.getBankId(),
                query.getQuestionType(),
                keyword
        );
        List<QuestionManagementResponse> items = questionMapper.findByOwner(
                        ownerId,
                        query.getBankId(),
                        query.getQuestionType(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::managementResponse)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public QuestionManagementResponse create(
            Long creatorId,
            boolean creatorAdmin,
            QuestionWriteRequest request
    ) {
        QuestionBank bank = bankService.getRequired(request.bankId());
        bankService.assertOwnerOrAdmin(bank, creatorId, creatorAdmin);
        NormalizedQuestion normalized = validateAndNormalize(request);

        Question question = new Question();
        question.setCreatorId(creatorId);
        question.setStatus(QuestionStatus.ACTIVE);
        apply(question, request, normalized.answer());
        if (questionMapper.insert(question) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建题目失败");
        }
        replaceOptions(question.getId(), normalized.options(), normalized.correctOptionKeys());
        return managementResponse(getRequired(question.getId()));
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public QuestionManagementResponse detail(
            Long questionId,
            Long requesterId,
            boolean requesterAdmin
    ) {
        Question question = getRequired(questionId);
        assertOwnerOrAdmin(question, requesterId, requesterAdmin);
        return managementResponse(question);
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public QuestionManagementResponse update(
            Long questionId,
            Long requesterId,
            boolean requesterAdmin,
            QuestionWriteRequest request
    ) {
        Question question = getRequired(questionId);
        assertOwnerOrAdmin(question, requesterId, requesterAdmin);
        QuestionBank targetBank = bankService.getRequired(request.bankId());
        bankService.assertOwnerOrAdmin(targetBank, requesterId, requesterAdmin);
        NormalizedQuestion normalized = validateAndNormalize(request);

        apply(question, request, normalized.answer());
        if (questionMapper.update(question) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        replaceOptions(questionId, normalized.options(), normalized.correctOptionKeys());
        return managementResponse(getRequired(questionId));
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long questionId, Long requesterId, boolean requesterAdmin) {
        Question question = getRequired(questionId);
        assertOwnerOrAdmin(question, requesterId, requesterAdmin);
        if (questionMapper.softDelete(questionId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
    }

    /**
     * 供后续组卷和考试作答接口复用的安全投影。
     */
    public CandidateQuestionResponse candidateProjection(Long questionId) {
        Question question = getRequired(questionId);
        return new CandidateQuestionResponse(
                question.getId(),
                question.getQuestionType(),
                question.getStem(),
                safeOptions(questionId),
                question.getDefaultScore()
        );
    }

    /** 返回Required。 */
    public Question getRequired(Long questionId) {
        return questionMapper.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "题目不存在"));
    }

    /** 校验OwnerOr管理及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertOwnerOrAdmin(Question question, Long requesterId, boolean requesterAdmin) {
        QuestionBank bank = bankService.getRequired(question.getBankId());
        bankService.assertOwnerOrAdmin(bank, requesterId, requesterAdmin);
    }

    /** 执行 apply 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void apply(Question question, QuestionWriteRequest request, QuestionAnswer answer) {
        question.setBankId(request.bankId());
        question.setQuestionType(request.questionType());
        question.setStem(request.stem().trim());
        question.setAnswerJson(writeAnswer(answer));
        question.setAnswerText(request.questionType() == QuestionType.SHORT_ANSWER
                ? answer.acceptedAnswers().get(0).get(0)
                : null);
        question.setAnalysis(normalize(request.analysis()));
        question.setDefaultScore(request.defaultScore());
        question.setFillBlankAutoGradable(
                request.questionType() == QuestionType.FILL_BLANK
                        && Boolean.TRUE.equals(request.fillBlankAutoGradable())
        );
        question.setCaseSensitive(
                request.questionType() == QuestionType.FILL_BLANK
                        && Boolean.TRUE.equals(request.caseSensitive())
        );
    }

    /** 校验AndNormalize及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateAndNormalize(QuestionWriteRequest request) {
        QuestionAnswer answer = normalizeAnswer(request.answer());
        List<QuestionOptionWriteRequest> options = normalizeOptions(request.options());

        return switch (request.questionType()) {
            case SINGLE_CHOICE -> validateSingleChoice(answer, options);
            case MULTIPLE_CHOICE -> validateMultipleChoice(answer, options);
            case TRUE_FALSE -> validateTrueFalse(answer, options);
            case FILL_BLANK -> validateFillBlank(answer, options);
            case SHORT_ANSWER -> validateShortAnswer(answer, options);
        };
    }

    /** 校验SingleChoice及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateSingleChoice(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        validateChoiceOptions(options);
        if (answer.acceptedAnswers().size() != 1
                || answer.acceptedAnswers().get(0).size() != 1) {
            throw invalidAnswer("单选题必须且只能有一个正确选项");
        }
        Set<String> correctKeys = normalizeAndValidateChoiceKeys(answer, options);
        return new NormalizedQuestion(options, answerWithKeys(correctKeys), correctKeys);
    }

    /** 校验MultipleChoice及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateMultipleChoice(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        validateChoiceOptions(options);
        if (answer.acceptedAnswers().size() != 1
                || answer.acceptedAnswers().get(0).isEmpty()) {
            throw invalidAnswer("多选题至少需要一个正确选项");
        }
        Set<String> correctKeys = normalizeAndValidateChoiceKeys(answer, options);
        return new NormalizedQuestion(options, answerWithKeys(correctKeys), correctKeys);
    }

    /** 校验TrueFalse及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateTrueFalse(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        if (!options.isEmpty()) {
            throw invalidAnswer("判断题选项由系统生成，不需要提交options");
        }
        if (answer.acceptedAnswers().size() != 1
                || answer.acceptedAnswers().get(0).size() != 1) {
            throw invalidAnswer("判断题答案必须是TRUE或FALSE");
        }
        String value = answer.acceptedAnswers().get(0).get(0).toUpperCase(Locale.ROOT);
        if (!value.equals("TRUE") && !value.equals("FALSE")) {
            throw invalidAnswer("判断题答案必须是TRUE或FALSE");
        }
        List<QuestionOptionWriteRequest> generated = List.of(
                new QuestionOptionWriteRequest("TRUE", "正确", 0),
                new QuestionOptionWriteRequest("FALSE", "错误", 1)
        );
        Set<String> correctKeys = Set.of(value);
        return new NormalizedQuestion(generated, answerWithKeys(correctKeys), correctKeys);
    }

    /** 校验FillBlank及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateFillBlank(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        requireNoOptions(options, "填空题");
        if (answer.acceptedAnswers().isEmpty()) {
            throw invalidAnswer("填空题至少需要一个空的答案");
        }
        return new NormalizedQuestion(options, answer, Set.of());
    }

    /** 校验Short答案及相关业务前置条件，不满足时抛出明确业务异常。 */
    private NormalizedQuestion validateShortAnswer(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        requireNoOptions(options, "简答题");
        if (answer.acceptedAnswers().size() != 1
                || answer.acceptedAnswers().get(0).size() != 1) {
            throw invalidAnswer("简答题必须提供一份参考答案");
        }
        return new NormalizedQuestion(options, answer, Set.of());
    }

    /** 转换或规范化答案数据，不引入额外持久化副作用。 */
    private QuestionAnswer normalizeAnswer(QuestionAnswer answer) {
        if (answer == null || answer.acceptedAnswers() == null) {
            throw invalidAnswer("正确答案不能为空");
        }
        List<List<String>> groups = new ArrayList<>();
        for (List<String> group : answer.acceptedAnswers()) {
            if (group == null || group.isEmpty()) {
                throw invalidAnswer("答案组不能为空");
            }
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (String value : group) {
                if (value == null || value.isBlank()) {
                    throw invalidAnswer("答案内容不能为空");
                }
                String normalized = value.trim();
                if (normalized.length() > 4000) {
                    throw invalidAnswer("单个答案不能超过4000个字符");
                }
                values.add(normalized);
            }
            groups.add(List.copyOf(values));
        }
        return new QuestionAnswer(groups);
    }

    /** 转换或规范化Options数据，不引入额外持久化副作用。 */
    private List<QuestionOptionWriteRequest> normalizeOptions(
            List<QuestionOptionWriteRequest> requestedOptions
    ) {
        if (requestedOptions == null) {
            return List.of();
        }
        List<QuestionOptionWriteRequest> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int index = 0; index < requestedOptions.size(); index++) {
            QuestionOptionWriteRequest option = requestedOptions.get(index);
            String key = option.key().trim().toUpperCase(Locale.ROOT);
            if (!keys.add(key)) {
                throw invalidAnswer("选项标识不能重复");
            }
            result.add(new QuestionOptionWriteRequest(
                    key,
                    option.text().trim(),
                    option.sortOrder() == null ? index : option.sortOrder()
            ));
        }
        return List.copyOf(result);
    }

    /** 校验ChoiceOptions及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateChoiceOptions(List<QuestionOptionWriteRequest> options) {
        if (options.size() < MIN_CHOICE_OPTIONS) {
            throw invalidAnswer("选择题至少需要两个选项");
        }
    }

    /** 转换或规范化AndValidateChoiceKeys数据，不引入额外持久化副作用。 */
    private Set<String> normalizeAndValidateChoiceKeys(
            QuestionAnswer answer,
            List<QuestionOptionWriteRequest> options
    ) {
        Set<String> optionKeys = options.stream()
                .map(QuestionOptionWriteRequest::key)
                .collect(java.util.stream.Collectors.toSet());
        LinkedHashSet<String> correctKeys = new LinkedHashSet<>();
        for (String key : answer.acceptedAnswers().get(0)) {
            String normalized = key.toUpperCase(Locale.ROOT);
            if (!optionKeys.contains(normalized)) {
                throw invalidAnswer("正确答案引用了不存在的选项：" + normalized);
            }
            correctKeys.add(normalized);
        }
        return Set.copyOf(correctKeys);
    }

    /** 执行 answerWithKeys 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private QuestionAnswer answerWithKeys(Set<String> correctKeys) {
        List<String> sorted = correctKeys.stream().sorted().toList();
        return new QuestionAnswer(List.of(sorted));
    }

    /** 校验NoOptions及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void requireNoOptions(List<QuestionOptionWriteRequest> options, String typeName) {
        if (!options.isEmpty()) {
            throw invalidAnswer(typeName + "不能包含选项");
        }
    }

    /** 更新Options，通过返回值或版本条件识别并发状态变化。 */
    private void replaceOptions(
            Long questionId,
            List<QuestionOptionWriteRequest> options,
            Set<String> correctKeys
    ) {
        optionMapper.deleteByQuestionId(questionId);
        for (QuestionOptionWriteRequest request : options) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(request.key());
            option.setOptionText(request.text());
            option.setCorrect(correctKeys.contains(request.key()));
            option.setSortOrder(request.sortOrder());
            if (optionMapper.insert(option) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存题目选项失败");
            }
        }
    }

    /** 执行 managementResponse 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private QuestionManagementResponse managementResponse(Question question) {
        return new QuestionManagementResponse(
                question.getId(),
                question.getBankId(),
                question.getCreatorId(),
                question.getQuestionType(),
                question.getStem(),
                safeOptions(question.getId()),
                readAnswer(question.getAnswerJson()),
                question.getAnalysis(),
                question.getDefaultScore(),
                Boolean.TRUE.equals(question.getFillBlankAutoGradable()),
                Boolean.TRUE.equals(question.getCaseSensitive()),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    /** 执行 safeOptions 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private List<QuestionOptionResponse> safeOptions(Long questionId) {
        return optionMapper.findByQuestionId(questionId).stream()
                .map(QuestionOptionResponse::from)
                .toList();
    }

    /** 执行 writeAnswer 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String writeAnswer(QuestionAnswer answer) {
        try {
            return objectMapper.writeValueAsString(answer);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "正确答案序列化失败");
        }
    }

    /** 查询答案相关数据；只返回当前调用方有权查看的结果。 */
    private QuestionAnswer readAnswer(String answerJson) {
        try {
            return objectMapper.readValue(answerJson, QuestionAnswer.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "题目答案数据格式损坏");
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 invalidAnswer 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalidAnswer(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    /** 转换或规范化d题目数据，不引入额外持久化副作用。 */
    private record NormalizedQuestion(
            List<QuestionOptionWriteRequest> options,
            QuestionAnswer answer,
            Set<String> correctOptionKeys
    ) {
    }
}
