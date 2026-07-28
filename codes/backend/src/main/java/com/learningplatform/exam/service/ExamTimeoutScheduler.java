/* 文件职责：按配置周期调度考试超时调度器，并把实际业务处理委托给服务层。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import com.learningplatform.exam.mapper.ExamAttemptMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
/**
 * 按配置周期调度考试超时调度器，并把实际业务处理委托给服务层。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamTimeoutScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExamTimeoutScheduler.class);
    /** 定义 BATCH_SIZE 常量，统一该组件使用的固定规则或默认值。 */
    private static final int BATCH_SIZE = 100;

    /** 访问作答持久化数据。 */
    private final ExamAttemptMapper attemptMapper;
    /** 委托交卷执行对应领域规则。 */
    private final ExamSubmissionService submissionService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamTimeoutScheduler(
            ExamAttemptMapper attemptMapper,
            ExamSubmissionService submissionService
    ) {
        this.attemptMapper = attemptMapper;
        this.submissionService = submissionService;
    }

    @Scheduled(
            fixedDelayString = "${app.exam.timeout-scan-ms:5000}",
            initialDelayString = "${app.exam.timeout-scan-initial-delay-ms:5000}"
    )
    /** 执行 scan 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public void scan() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        List<Long> attemptIds = attemptMapper.findExpiredIds(now, BATCH_SIZE);
        int submitted = 0;
        for (Long attemptId : attemptIds) {
            try {
                if (submissionService.submitExpired(attemptId)) {
                    submitted++;
                }
            } catch (RuntimeException exception) {
                log.error("Failed to auto-submit expired exam attempt {}", attemptId, exception);
            }
        }
        if (submitted > 0) {
            log.info("Auto-submitted {} expired exam attempt(s)", submitted);
        }
    }
}
