/* 文件职责：实现AI成绩持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiClientResponse;
import com.learningplatform.ai.domain.AiConversation;
import com.learningplatform.ai.domain.AiMessage;
import com.learningplatform.ai.domain.AiMessageRole;
import com.learningplatform.ai.domain.AiSummary;
import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.mapper.AiConversationMapper;
import com.learningplatform.ai.mapper.AiMessageMapper;
import com.learningplatform.ai.mapper.AiSummaryMapper;
import com.learningplatform.ai.mapper.AiTaskMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
/**
 * 实现AI成绩持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AiResultPersistenceService {
    /** 访问任务持久化数据。 */
    private final AiTaskMapper taskMapper;
    /** 访问总结持久化数据。 */
    private final AiSummaryMapper summaryMapper;
    /** 访问会话持久化数据。 */
    private final AiConversationMapper conversationMapper;
    /** 访问消息持久化数据。 */
    private final AiMessageMapper messageMapper;
    /** 委托额度执行对应领域规则。 */
    private final AiQuotaService quotaService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiResultPersistenceService(
            AiTaskMapper taskMapper,
            AiSummaryMapper summaryMapper,
            AiConversationMapper conversationMapper,
            AiMessageMapper messageMapper,
            AiQuotaService quotaService
    ) {
        this.taskMapper = taskMapper;
        this.summaryMapper = summaryMapper;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.quotaService = quotaService;
    }

    @Transactional
    /** 更新总结，通过返回值或版本条件识别并发状态变化。 */
    public AiSummary saveSummary(
            AiTask task,
            Long contentId,
            String summaryText,
            String knowledgePointsJson,
            String reviewOutline,
            String sourceVersion
    ) {
        AiSummary summary = new AiSummary();
        summary.setTaskId(task.getId());
        summary.setContentId(contentId);
        summary.setSummaryText(summaryText);
        summary.setKnowledgePointsJson(knowledgePointsJson);
        summary.setReviewOutline(reviewOutline);
        summary.setSourceVersion(sourceVersion);
        if (summaryMapper.insert(summary) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存 AI 总结失败");
        }
        quotaService.consume(task);
        markSucceeded(task.getId());
        return summaryMapper.findByTaskId(task.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存后无法读取 AI 总结"
                ));
    }

    @Transactional
    /** 更新讲解，通过返回值或版本条件识别并发状态变化。 */
    public ExplanationMessages saveExplanation(
            AiTask task,
            Long conversationId,
            Long userId,
            String question,
            AiClientResponse response
    ) {
        AiConversation conversation = conversationMapper.findByIdAndUserForUpdate(
                        conversationId,
                        userId
                )
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "AI 会话不存在"
                ));
        int sequence = messageMapper.maxSequenceNo(conversationId);

        AiMessage questionMessage = new AiMessage();
        questionMessage.setConversationId(conversationId);
        questionMessage.setRole(AiMessageRole.USER);
        questionMessage.setContent(question);
        questionMessage.setSequenceNo(sequence + 1);
        if (messageMapper.insert(questionMessage) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存用户问题失败");
        }

        AiMessage answerMessage = new AiMessage();
        answerMessage.setConversationId(conversationId);
        answerMessage.setTaskId(task.getId());
        answerMessage.setRole(AiMessageRole.ASSISTANT);
        answerMessage.setContent(response.content());
        answerMessage.setSequenceNo(sequence + 2);
        answerMessage.setTokenCount(response.completionTokens());
        if (messageMapper.insert(answerMessage) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存 AI 讲解失败");
        }

        LocalDateTime finishedAt = now();
        if (conversationMapper.touch(
                conversation.getId(),
                userId,
                finishedAt
        ) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新 AI 会话失败");
        }
        quotaService.consume(task);
        markSucceeded(task.getId());

        List<AiMessage> saved = messageMapper.findByConversationId(conversationId);
        AiMessage savedQuestion = findById(saved, questionMessage.getId());
        AiMessage savedAnswer = findById(saved, answerMessage.getId());
        AiTask savedTask = taskMapper.findByIdAndUser(task.getId(), userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "完成后无法读取 AI 任务"
                ));
        return new ExplanationMessages(savedTask, savedQuestion, savedAnswer);
    }

    /** 执行 markSucceeded 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void markSucceeded(Long taskId) {
        if (taskMapper.markSucceeded(taskId, now()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 任务状态已变化");
        }
    }

    /** 按ID查询数据；只返回当前调用方有权查看的结果。 */
    private AiMessage findById(List<AiMessage> messages, Long id) {
        return messages.stream()
                .filter(message -> message.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存后无法读取 AI 消息"
                ));
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** 执行 ExplanationMessages 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public record ExplanationMessages(
            AiTask task,
            AiMessage question,
            AiMessage answer
    ) {
    }
}
