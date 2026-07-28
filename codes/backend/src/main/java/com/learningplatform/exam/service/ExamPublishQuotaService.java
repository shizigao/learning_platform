/* 文件职责：实现考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

/**
 * 实现考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public interface ExamPublishQuotaService {
    /** 执行消费核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    void consume(Long publisherId, Long examId);

    /** 执行 availableQuota 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    int availableQuota(Long publisherId);
}
