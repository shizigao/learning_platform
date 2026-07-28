/* 文件职责：实现AI总结业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.client.AiClientException;
import com.learningplatform.ai.client.AiClientRequest;
import com.learningplatform.ai.client.AiClientResponse;
import com.learningplatform.ai.client.AiMessage;
import com.learningplatform.ai.client.AiResponseFormat;
import com.learningplatform.ai.client.AiRole;
import com.learningplatform.ai.domain.AiSummary;
import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskStatus;
import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.dto.AiSummaryResponse;
import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.ai.mapper.AiSummaryMapper;
import com.learningplatform.ai.text.ContentTextExtractor;
import com.learningplatform.ai.text.ExtractedContentText;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.service.ContentAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 实现AI总结业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AiSummaryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryService.class);
    /** 约束 AI 的任务、输出格式和安全边界，防止执行用户数据中的指令。 */
    private static final String SYSTEM_PROMPT = """
            TASK:CONTENT_SUMMARY
            你是严谨的中文学习资料总结助手。仅依据用户提供的资料文本生成结果，
            不得补充资料中不存在的事实。只输出 JSON，不要输出 Markdown 代码块。
            摘要不超过 300 个中文字符；提取 3～8 个知识点；复习提纲不超过
            500 个中文字符，避免重复和冗长推理。
            JSON 格式：
            {"summary":"内容摘要","knowledgePoints":["知识点1"],"reviewOutline":"复习提纲"}
            """;

    /** 通过AIClient调用隔离后的外部能力。 */
    private final AiClient aiClient;
    /** 保存text提取器，供该类型的业务逻辑读取或更新。 */
    private final ContentTextExtractor textExtractor;
    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;
    /** 委托任务执行对应领域规则。 */
    private final AiTaskLifecycleService taskService;
    /** 委托持久化执行对应领域规则。 */
    private final AiResultPersistenceService persistenceService;
    /** 访问总结持久化数据。 */
    private final AiSummaryMapper summaryMapper;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;
    /** 委托额度执行对应领域规则。 */
    private final AiQuotaService quotaService;
    /** 保存请求保护，供该类型的业务逻辑读取或更新。 */
    private final AiRequestGuard requestGuard;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiSummaryService(
            AiClient aiClient,
            ContentTextExtractor textExtractor,
            ContentAccessService accessService,
            AiTaskLifecycleService taskService,
            AiResultPersistenceService persistenceService,
            AiSummaryMapper summaryMapper,
            ObjectMapper objectMapper,
            AiQuotaService quotaService,
            AiRequestGuard requestGuard
    ) {
        this.aiClient = aiClient;
        this.textExtractor = textExtractor;
        this.accessService = accessService;
        this.taskService = taskService;
        this.persistenceService = persistenceService;
        this.summaryMapper = summaryMapper;
        this.objectMapper = objectMapper;
        this.quotaService = quotaService;
        this.requestGuard = requestGuard;
    }

    /** 执行生成核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public AiSummaryResponse generate(
            Long contentId,
            Long userId,
            boolean requesterAdmin,
            String requestId
    ) {
        // 从contentId对应的学习资料中提取纯文本信息，点击extract
        ExtractedContentText source = textExtractor.extract(
                contentId,
                userId,
                requesterAdmin
        );
        // 创建一个AI任务task，任务类型为summary（保障AI任务顺利完整执行）
        AiTaskLifecycleService.TaskCreation creation = taskService.create(
                requestId,
                userId,
                contentId,
                null,
                AiTaskType.SUMMARY,
                source.text().length()
        );
        if (!creation.created()) {
            return existingResult(creation.task(), userId);
        }
        // 记录日志
        LOGGER.info(
                "AI_SUMMARY_START traceId={} taskId={} requestId={} userId={} "
                        + "contentId={} inputChars={}",
                traceId(),
                creation.task().getId(),
                requestId,
                userId,
                contentId,
                source.text().length()
        );
        try {
            quotaService.requireAvailable(userId, creation.task().getQuotaCost());
            // 开始任务task
            AiTask running = taskService.start(creation.task().getId(), userId);
            // 生成AI回复
            AiClientResponse response = requestGuard.execute(
                    userId,
                    // aiClient.complete方法为底层调用API的方法
                    () -> aiClient.complete(new AiClientRequest(
                            List.of(
                                    // 此处SYSTEM_PROMPT为AI总结的系统提示词
                                    new AiMessage(AiRole.SYSTEM, SYSTEM_PROMPT),
                                    // 此处source.text()为资料中的文本内容
                                    new AiMessage(AiRole.USER, source.text())
                            ),
                            1200,
                            0.2,
                            AiResponseFormat.JSON_OBJECT
                    ))
            );
            // 生成好回复后需要进行格式转换
            GeneratedSummary generated = parse(response.content());
            String pointsJson = objectMapper.writeValueAsString(
                    generated.knowledgePoints()
            );
            // 存储AI总结
            AiSummary saved = persistenceService.saveSummary(
                    running,
                    contentId,
                    generated.summary(),
                    pointsJson,
                    generated.reviewOutline(),
                    source.sourceVersion()
            );
            LOGGER.info(
                    "AI_SUMMARY_SUCCESS traceId={} taskId={} provider={} model={} "
                            + "summaryChars={} knowledgePoints={} outlineChars={}",
                    traceId(),
                    running.getId(),
                    response.provider(),
                    response.model(),
                    generated.summary().length(),
                    generated.knowledgePoints().size(),
                    generated.reviewOutline().length()
            );
            // 返回结果
            return response(saved, taskService.require(running.getId(), userId));
        } catch (AiRequestGuard.GuardException exception) {
            LOGGER.warn(
                    "AI_SUMMARY_FAILURE traceId={} taskId={} stage=guard kind={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getFailure()
            );
            taskService.fail(
                    creation.task().getId(),
                    exception.getFailure().name(),
                    exception.getMessage()
            );
            throw exception;
        } catch (AiClientException exception) {
            LOGGER.warn(
                    "AI_SUMMARY_FAILURE traceId={} taskId={} stage=provider kind={} "
                            + "message={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getKind(),
                    exception.getMessage()
            );
            taskService.fail(
                    creation.task().getId(),
                    exception.getKind().name(),
                    "AI 服务暂时不可用，请稍后重试"
            );
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 总结生成失败，请稍后重试"
            );
        } catch (BusinessException exception) {
            LOGGER.warn(
                    "AI_SUMMARY_FAILURE traceId={} taskId={} stage=business code={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getErrorCode()
            );
            taskService.fail(
                    creation.task().getId(),
                    businessErrorCode(exception),
                    safeTaskMessage(exception)
            );
            if (exception.getErrorCode() == ErrorCode.INTERNAL_ERROR) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "AI 总结生成失败，请稍后重试"
                );
            }
            throw exception;
        } catch (RuntimeException | JsonProcessingException exception) {
            LOGGER.warn(
                    "AI_SUMMARY_FAILURE traceId={} taskId={} "
                            + "stage=result-processing exception={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getClass().getSimpleName()
            );
            taskService.fail(
                    creation.task().getId(),
                    "RESULT_PROCESSING_FAILED",
                    "AI 返回结果处理失败"
            );
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 总结生成失败，请稍后重试"
            );
        }
    }

    /** 执行 traceId 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "-" : value;
    }

    /** 执行 businessErrorCode 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String businessErrorCode(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.FORBIDDEN
                ? "AI_QUOTA_INSUFFICIENT"
                : "AI_BUSINESS_FAILURE";
    }

    /** 执行 safeTaskMessage 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String safeTaskMessage(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.FORBIDDEN
                ? "AI 可用次数不足"
                : "AI 任务处理失败";
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public AiSummaryResponse latest(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        AiSummary summary = summaryMapper.findLatestByContentAndUser(
                        contentId,
                        userId
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "暂无 AI 总结"
                ));
        return response(summary, taskService.require(summary.getTaskId(), userId));
    }

    /** 执行 existingResult 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private AiSummaryResponse existingResult(AiTask task, Long userId) {
        if (task.getStatus() == AiTaskStatus.SUCCEEDED) {
            AiSummary summary = summaryMapper.findByTaskId(task.getId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "AI 任务缺少总结结果"
                    ));
            return response(summary, task);
        }
        if (task.getStatus() == AiTaskStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "该 AI 请求已失败，请使用新的请求幂等号重试"
            );
        }
        throw new BusinessException(ErrorCode.CONFLICT, "AI 总结任务正在处理中");
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private AiSummaryResponse response(AiSummary summary, AiTask task) {
        try {
            List<String> knowledgePoints = objectMapper.readValue(
                    summary.getKnowledgePointsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            String.class
                    )
            );
            return new AiSummaryResponse(
                    summary.getId(),
                    AiTaskResponse.from(task),
                    summary.getContentId(),
                    summary.getSummaryText(),
                    knowledgePoints,
                    summary.getReviewOutline(),
                    summary.getSourceVersion(),
                    summary.getCreatedAt()
            );
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 总结数据损坏"
            );
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private GeneratedSummary parse(String content) throws JsonProcessingException {
        String json = extractJson(content);
        GeneratedSummary result = objectMapper.readValue(
                json,
                GeneratedSummary.class
        );
        if (result.summary() == null || result.summary().isBlank()
                || result.reviewOutline() == null || result.reviewOutline().isBlank()
                || result.knowledgePoints() == null
                || result.knowledgePoints().isEmpty()
                || result.knowledgePoints().stream().anyMatch(
                        point -> point == null || point.isBlank()
                )) {
            throw new JsonProcessingException("AI 总结字段不完整") {
            };
        }
        return new GeneratedSummary(
                result.summary().trim(),
                result.knowledgePoints().stream().map(String::trim).toList(),
                result.reviewOutline().trim()
        );
    }

    /** 执行 extractJson 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String extractJson(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end > start
                ? value.substring(start, end + 1)
                : value;
    }

    /** 执行Generated总结核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    private record GeneratedSummary(
            String summary,
            List<String> knowledgePoints,
            String reviewOutline
    ) {
    }
}
