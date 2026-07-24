package com.learningplatform.admin.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    String COLUMNS = """
            id, operator_id, operator_name, module, action, target_type,
            target_id, request_method, request_path, request_id, ip_address,
            user_agent, result, detail_json, error_message, duration_ms, created_at
            """;

    @Insert("""
            INSERT INTO operation_log (
                operator_id, operator_name, module, action, target_type,
                target_id, request_method, request_path, request_id,
                ip_address, user_agent, result, detail_json, error_message,
                duration_ms
            ) VALUES (
                #{operatorId}, #{operatorName}, #{module}, #{action},
                #{targetType}, #{targetId}, #{requestMethod}, #{requestPath},
                #{requestId}, #{ipAddress}, #{userAgent}, #{result},
                #{detailJson}, #{errorMessage}, #{durationMs}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog operationLog);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM operation_log
            WHERE 1 = 1
            <if test='operatorId != null'>AND operator_id = #{operatorId}</if>
            <if test='module != null'>AND module = #{module}</if>
            <if test='action != null'>AND action = #{action}</if>
            <if test='result != null'>AND result = #{result}</if>
            <if test='requestId != null'>AND request_id = #{requestId}</if>
            </script>
            """)
    long count(
            @Param("operatorId") Long operatorId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("result") OperationResult result,
            @Param("requestId") String requestId
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM operation_log
            WHERE 1 = 1
            <if test='operatorId != null'>AND operator_id = #{operatorId}</if>
            <if test='module != null'>AND module = #{module}</if>
            <if test='action != null'>AND action = #{action}</if>
            <if test='result != null'>AND result = #{result}</if>
            <if test='requestId != null'>AND request_id = #{requestId}</if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<OperationLog> find(
            @Param("operatorId") Long operatorId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("result") OperationResult result,
            @Param("requestId") String requestId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );
}
