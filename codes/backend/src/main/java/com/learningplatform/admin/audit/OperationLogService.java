package com.learningplatform.admin.audit;

import com.learningplatform.admin.dto.OperationLogListQuery;
import com.learningplatform.admin.dto.OperationLogResponse;
import com.learningplatform.common.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;

    public OperationLogService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
