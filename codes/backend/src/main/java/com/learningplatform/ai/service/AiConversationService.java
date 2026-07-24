package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.client.AiClientException;
import com.learningplatform.ai.client.AiClientRequest;
import com.learningplatform.ai.client.AiClientResponse;
import com.learningplatform.ai.domain.AiConversation;
import com.learningplatform.ai.domain.AiConversationStatus;
import com.learningplatform.ai.domain.AiMessage;
import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskStatus;
import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.dto.AiConversationResponse;
import com.learningplatform.ai.dto.AiExplanationResponse;
import com.learningplatform.ai.dto.AiMessageResponse;
import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.ai.mapper.AiConversationMapper;
import com.learningplatform.ai.mapper.AiMessageMapper;
import com.learningplatform.ai.text.ContentTextExtractor;
import com.learningplatform.ai.text.ExtractedContentText;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.service.ContentAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiConversationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AiConversationService.class
    );
    private final AiClient aiClient;
    private final ContentTextExtractor textExtractor;
    private final ContentAccessService accessService;
    private final AiTaskLifecycleService taskService;
    private final AiResultPersistenceService persistenceService;
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiConversationPromptFactory promptFactory;
    private final AiQuotaService quotaService;
    private final AiRequestGuard requestGuard;

    public AiConversationService(
            AiClient aiClient,
            ContentTextExtractor textExtractor,
            ContentAccessService accessService,
            AiTaskLifecycleService taskService,
            AiResultPersistenceService persistenceService,
            AiConversationMapper conversationMapper,
            AiMessageMapper messageMapper,
            AiConversationPromptFactory promptFactory,
            AiQuotaService quotaService,
            AiRequestGuard requestGuard
    ) {
        this.aiClient = aiClient;
        this.textExtractor = textExtractor;
        this.accessService = accessService;
        this.taskService = taskService;
        this.persistenceService = persistenceService;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.promptFactory = promptFactory;
        this.quotaService = quotaService;
        this.requestGuard = requestGuard;
    }

    @Transactional
    public AiConversationResponse create(
            Long contentId,
            Long userId,
            boolean requesterAdmin,
            String requestedTitle
    ) {
        ExtractedContentText source = textExtractor.extract(
                contentId,
                userId,
                requesterAdmin
        );
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setContentId(contentId);
        conversation.setTitle(title(requestedTitle, source.title()));
        conversation.setStatus(AiConversationStatus.ACTIVE);
        if (conversationMapper.insert(conversation) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建 AI 会话失败");
        }
        return detail(conversation.getId(), userId, requesterAdmin);
    }

    public List<AiConversationResponse> list(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        return conversationMapper.findByUserAndContent(userId, contentId).stream()
                .map(conversation -> response(conversation, List.of()))
                .toList();
    }

    public AiConversationResponse detail(
            Long conversationId,
            Long userId,
            boolean requesterAdmin
    ) {
        AiConversation conversation = requireOwned(conversationId, userId);
        accessService.requireAccess(
                conversation.getContentId(),
                userId,
                requesterAdmin
        );
        return response(
                conversation,
                messageMapper.findByConversationId(conversationId)
        );
    }

    public AiExplanationResponse explain(
            Long conversationId,
            Long userId,
            boolean requesterAdmin,
            String requestId,
            String question
    ) {
        AiConversation conversation = requireOwned(conversationId, userId);
        if (conversation.getStatus() != AiConversationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 会话已归档");
        }
        ExtractedContentText source = textExtractor.extract(
                conversation.getContentId(),
                userId,
                requesterAdmin
        );
        String normalizedQuestion = question.trim();
        AiTaskLifecycleService.TaskCreation creation = taskService.create(
                requestId,
                userId,
                conversation.getContentId(),
                conversationId,
                AiTaskType.EXPLANATION,
                source.text().length() + normalizedQuestion.length()
        );
        if (!creation.created()) {
            return existingExplanation(creation.task(), userId);
        }
        LOGGER.info(
                "AI_EXPLANATION_START traceId={} taskId={} requestId={} userId={} "
                        + "contentId={} conversationId={} sourceChars={} questionChars={}",
                traceId(),
                creation.task().getId(),
                requestId,
                userId,
                conversation.getContentId(),
                conversationId,
                source.text().length(),
                normalizedQuestion.length()
        );
        try {
            quotaService.requireAvailable(userId, creation.task().getQuotaCost());
            AiTask running = taskService.start(creation.task().getId(), userId);
            List<com.learningplatform.ai.client.AiMessage> requestMessages =
                    promptFactory.build(
                            source,
                            messageMapper.findByConversationId(conversationId),
                            normalizedQuestion
                    );
            AiClientResponse clientResponse = requestGuard.execute(
                    userId,
                    () -> aiClient.complete(
                            new AiClientRequest(requestMessages, 1000, 0.2)
                    )
            );
            AiResultPersistenceService.ExplanationMessages saved =
                    persistenceService.saveExplanation(
                            running,
                            conversationId,
                            userId,
                            normalizedQuestion,
                            clientResponse
                    );
            LOGGER.info(
                    "AI_EXPLANATION_SUCCESS traceId={} taskId={} provider={} model={} "
                            + "answerChars={} contextMessages={}",
                    traceId(),
                    running.getId(),
                    clientResponse.provider(),
                    clientResponse.model(),
                    saved.answer().getContent().length(),
                    requestMessages.size()
            );
            return explanationResponse(saved);
        } catch (AiRequestGuard.GuardException exception) {
            LOGGER.warn(
                    "AI_EXPLANATION_FAILURE traceId={} taskId={} stage=guard kind={}",
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
                    "AI_EXPLANATION_FAILURE traceId={} taskId={} stage=provider kind={} "
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
                    "AI 知识讲解失败，请稍后重试"
            );
        } catch (BusinessException exception) {
            LOGGER.warn(
                    "AI_EXPLANATION_FAILURE traceId={} taskId={} stage=business code={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getErrorCode()
            );
            taskService.fail(
                    creation.task().getId(),
                    exception.getErrorCode() == ErrorCode.FORBIDDEN
                            ? "AI_QUOTA_INSUFFICIENT"
                            : "AI_BUSINESS_FAILURE",
                    exception.getErrorCode() == ErrorCode.FORBIDDEN
                            ? "AI 可用次数不足"
                            : "AI 任务处理失败"
            );
            if (exception.getErrorCode() == ErrorCode.INTERNAL_ERROR) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "AI 知识讲解失败，请稍后重试"
                );
            }
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "AI_EXPLANATION_FAILURE traceId={} taskId={} "
                            + "stage=result-persistence exception={}",
                    traceId(),
                    creation.task().getId(),
                    exception.getClass().getSimpleName()
            );
            taskService.fail(
                    creation.task().getId(),
                    "RESULT_PERSIST_FAILED",
                    "AI 讲解结果保存失败"
            );
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 知识讲解失败，请稍后重试"
            );
        }
    }

    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "-" : value;
    }

    private AiExplanationResponse existingExplanation(AiTask task, Long userId) {
        if (task.getStatus() == AiTaskStatus.SUCCEEDED) {
            AiMessage answer = messageMapper.findAssistantByTaskId(task.getId())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "AI 任务缺少讲解结果"
                    ));
            List<AiMessage> messages = messageMapper.findByConversationId(
                    task.getConversationId()
            );
            AiMessage question = messages.stream()
                    .filter(message -> message.getSequenceNo()
                            == answer.getSequenceNo() - 1)
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "AI 任务缺少用户问题"
                    ));
            return new AiExplanationResponse(
                    AiTaskResponse.from(task),
                    task.getConversationId(),
                    AiMessageResponse.from(question),
                    AiMessageResponse.from(answer)
            );
        }
        if (task.getStatus() == AiTaskStatus.FAILED) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "该 AI 请求已失败，请使用新的请求幂等号重试"
            );
        }
        throw new BusinessException(ErrorCode.CONFLICT, "AI 讲解任务正在处理中");
    }

    private AiExplanationResponse explanationResponse(
            AiResultPersistenceService.ExplanationMessages saved
    ) {
        return new AiExplanationResponse(
                AiTaskResponse.from(saved.task()),
                saved.question().getConversationId(),
                AiMessageResponse.from(saved.question()),
                AiMessageResponse.from(saved.answer())
        );
    }

    private AiConversation requireOwned(Long conversationId, Long userId) {
        return conversationMapper.findByIdAndUser(conversationId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "AI 会话不存在"
                ));
    }

    private AiConversationResponse response(
            AiConversation conversation,
            List<AiMessage> messages
    ) {
        return new AiConversationResponse(
                conversation.getId(),
                conversation.getContentId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getLastMessageAt(),
                messages.stream().map(AiMessageResponse::from).toList(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private String title(String requested, String contentTitle) {
        String value = requested == null || requested.isBlank()
                ? contentTitle + " · AI讲解"
                : requested.trim();
        return value.length() <= 200 ? value : value.substring(0, 200);
    }
}
