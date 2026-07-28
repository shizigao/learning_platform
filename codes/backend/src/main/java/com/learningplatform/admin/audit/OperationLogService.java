/* 文件职责：实现操作日志业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：平台治理与管理员操作；所在分层：审计基础设施层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.audit;

import com.learningplatform.admin.dto.OperationLogListQuery;
import com.learningplatform.admin.dto.OperationLogResponse;
import com.learningplatform.common.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/**
 * 实现操作日志业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：遵守 平台治理与管理员操作 模块的职责边界。</p>
 */
public class OperationLogService {
    /** 访问操作日志持久化数据。 */
    private final OperationLogMapper operationLogMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    /** 执行 record 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public void record(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<OperationLogResponse> list(OperationLogListQuery query) {
        String module = normalize(query.getModule());
        String action = normalize(query.getAction());
        String requestId = normalize(query.getRequestId());
        long total = operationLogMapper.count(
                query.getOperatorId(),
                module,
                action,
                query.getResult(),
                requestId
        );
        List<OperationLogResponse> items = operationLogMapper.find(
                        query.getOperatorId(),
                        module,
                        action,
                        query.getResult(),
                        requestId,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(OperationLogResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
