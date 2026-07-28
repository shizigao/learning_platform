/* 文件职责：定义操作日志的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：平台治理与管理员操作；所在分层：审计基础设施层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.audit;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义操作日志的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：遵守 平台治理与管理员操作 模块的职责边界。</p>
 */
public interface OperationLogMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
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
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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
    /** 执行 count 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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
    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
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
