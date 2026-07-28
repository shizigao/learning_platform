/* 文件职责：实现管理考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：平台治理与管理员操作；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.service;

import com.learningplatform.admin.dto.AdminExamDetailResponse;
import com.learningplatform.admin.dto.AdminExamListQuery;
import com.learningplatform.admin.dto.AdminExamSummaryResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.dto.ExamManagementResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.mapper.ExamMapper;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
/**
 * 实现管理考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AdminExamService {
    /** 访问考试持久化数据。 */
    private final ExamMapper examMapper;
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminExamService(
            ExamMapper examMapper,
            ExamService examService,
            UserService userService
    ) {
        this.examMapper = examMapper;
        this.examService = examService;
        this.userService = userService;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<AdminExamSummaryResponse> list(AdminExamListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = examMapper.countForAdmin(
                query.getPublisherId(),
                query.getStatus(),
                keyword
        );
        List<AdminExamSummaryResponse> items = examMapper.findForAdmin(
                        query.getPublisherId(),
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::summary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public AdminExamDetailResponse detail(Long operatorId, Long examId) {
        ExamManagementResponse management =
                examService.detail(examId, operatorId, true);
        User publisher = userService.getRequiredById(
                management.exam().publisherId()
        );
        return new AdminExamDetailResponse(
                management,
                publisher.getUsername(),
                publisher.getNickname()
        );
    }

    /** 执行 summary 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private AdminExamSummaryResponse summary(Exam exam) {
        User publisher = userService.getRequiredById(exam.getPublisherId());
        return AdminExamSummaryResponse.from(
                ExamSummaryResponse.from(exam),
                publisher
        );
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
