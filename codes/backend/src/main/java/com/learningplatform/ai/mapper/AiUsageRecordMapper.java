/* 文件职责：定义AI用量记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiUsageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义AI用量记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface AiUsageRecordMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, business_no, user_id, task_id, entitlement_id, usage_type,
            quantity, balance_before, balance_after, status, remark,
            created_at, updated_at
            """;

    @Insert("""
            INSERT INTO ai_usage_record (
                business_no, user_id, task_id, entitlement_id, usage_type,
                quantity, balance_before, balance_after, status, remark
            ) VALUES (
                #{businessNo}, #{userId}, #{taskId}, #{entitlementId}, #{usageType},
                #{quantity}, #{balanceBefore}, #{balanceAfter}, #{status}, #{remark}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(AiUsageRecord record);

    @Select("SELECT " + COLUMNS + " FROM ai_usage_record WHERE business_no = #{businessNo}")
    /** 执行 findByBusinessNo 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<AiUsageRecord> findByBusinessNo(String businessNo);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_usage_record
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    /** 执行 findByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<AiUsageRecord> findByUserId(Long userId);
}
