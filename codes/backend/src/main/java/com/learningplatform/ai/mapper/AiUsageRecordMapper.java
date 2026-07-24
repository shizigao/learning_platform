package com.learningplatform.ai.mapper;

import com.learningplatform.ai.domain.AiUsageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface AiUsageRecordMapper {
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
    int insert(AiUsageRecord record);

    @Select("SELECT " + COLUMNS + " FROM ai_usage_record WHERE business_no = #{businessNo}")
    Optional<AiUsageRecord> findByBusinessNo(String businessNo);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM ai_usage_record
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<AiUsageRecord> findByUserId(Long userId);
}
