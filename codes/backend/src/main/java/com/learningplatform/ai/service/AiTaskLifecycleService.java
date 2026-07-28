/* 文件职责：实现AI任务生命周期业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskStatus;
import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.ai.mapper.AiTaskMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.List;

/**
 * AI 任务的幂等创建与状态流转服务。
 *
 * <p>业务服务应先创建任务，仅在 {@link TaskCreation#created()} 为 true 时扣减额度
 * 并调用供应商；重复请求返回原任务，从而避免浏览器重试造成重复消费。</p>
 */
@Service
public class AiTaskLifecycleService {
    /** 访问任务持久化数据。 */
    private final AiTaskMapper taskMapper;
    /** 通过AIClient调用隔离后的外部能力。 */
    private final AiClient aiClient;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiTaskLifecycleService(AiTaskMapper taskMapper, AiClient aiClient) {
        this.taskMapper = taskMapper;
        this.aiClient = aiClient;
    }

    /**
     * 按请求幂等号创建 PENDING 任务。
     * 已存在的请求号必须与用户、资源、会话和任务类型完全一致。
     */
    @Transactional
    public TaskCreation create(
            String requestedId,
            Long userId,
            Long contentId,
            Long conversationId,
            AiTaskType taskType,
            int inputChars
    ) {
        String requestId = normalizeRequestId(requestedId);
        AiTask existing = taskMapper.findByRequestId(requestId).orElse(null);
        if (existing != null) {
            assertSameRequest(existing, userId, contentId, conversationId, taskType);
            return new TaskCreation(existing, false);
        }
        AiTask task = new AiTask();
        task.setRequestId(requestId);
        task.setUserId(userId);
        task.setContentId(contentId);
        task.setConversationId(conversationId);
        task.setTaskType(taskType);
        task.setProvider(aiClient.provider());
        task.setModel(aiClient.model());
        task.setStatus(AiTaskStatus.PENDING);
        task.setInputChars(inputChars);
        task.setQuotaCost(1);
        if (taskMapper.insert(task) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建 AI 任务失败");
        }
        return new TaskCreation(require(task.getId(), userId), true);
    }

    /** 原子地把 PENDING 任务标记为 RUNNING。 */
    @Transactional
    public AiTask start(Long taskId, Long userId) {
        if (taskMapper.markRunning(taskId, now()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 任务状态已变化");
        }
        return require(taskId, userId);
    }

    /** 保存截断后的安全错误信息；不得把密钥或供应商原始响应写入该字段。 */
    @Transactional
    public void fail(Long taskId, String errorCode, String safeMessage) {
        taskMapper.markFailed(
                taskId,
                limit(errorCode, 64),
                limit(safeMessage, 1000),
                now()
        );
    }

    /** 读取当前用户拥有的任务，不允许通过任务 ID 横向访问。 */
    public AiTask require(Long taskId, Long userId) {
        return taskMapper.findByIdAndUser(taskId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "AI 任务不存在"
                ));
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public AiTaskResponse detail(Long taskId, Long userId) {
        return AiTaskResponse.from(require(taskId, userId));
    }

    /** 按用户列出任务并转换为只读响应。 */
    public List<AiTaskResponse> list(Long userId) {
        return taskMapper.findByUserId(userId).stream()
                .map(AiTaskResponse::from)
                .toList();
    }

    /** 校验Same请求及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertSameRequest(
            AiTask task,
            Long userId,
            Long contentId,
            Long conversationId,
            AiTaskType taskType
    ) {
        if (!task.getUserId().equals(userId)
                || !java.util.Objects.equals(task.getContentId(), contentId)
                || !java.util.Objects.equals(task.getConversationId(), conversationId)
                || task.getTaskType() != taskType) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "请求幂等号已被其他 AI 任务使用"
            );
        }
    }

    /** 转换或规范化请求ID数据，不引入额外持久化副作用。 */
    private String normalizeRequestId(String value) {
        return value == null || value.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : value.trim();
    }

    /** 执行 limit 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 任务及“是否本次新建”的组合结果，是调用方执行扣次的幂等边界。 */
    public record TaskCreation(AiTask task, boolean created) {
    }
}
