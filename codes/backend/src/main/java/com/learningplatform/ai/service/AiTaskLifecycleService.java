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

@Service
public class AiTaskLifecycleService {
    private final AiTaskMapper taskMapper;
    private final AiClient aiClient;

    public AiTaskLifecycleService(AiTaskMapper taskMapper, AiClient aiClient) {
        this.taskMapper = taskMapper;
        this.aiClient = aiClient;
    }

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

    @Transactional
    public AiTask start(Long taskId, Long userId) {
        if (taskMapper.markRunning(taskId, now()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 任务状态已变化");
        }
        return require(taskId, userId);
    }

    @Transactional
    public void fail(Long taskId, String errorCode, String safeMessage) {
        taskMapper.markFailed(
                taskId,
                limit(errorCode, 64),
                limit(safeMessage, 1000),
                now()
        );
    }

    public AiTask require(Long taskId, Long userId) {
        return taskMapper.findByIdAndUser(taskId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "AI 任务不存在"
                ));
    }

    public AiTaskResponse detail(Long taskId, Long userId) {
        return AiTaskResponse.from(require(taskId, userId));
    }

    public List<AiTaskResponse> list(Long userId) {
        return taskMapper.findByUserId(userId).stream()
                .map(AiTaskResponse::from)
                .toList();
    }

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

    private String normalizeRequestId(String value) {
        return value == null || value.isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    public record TaskCreation(AiTask task, boolean created) {
    }
}
