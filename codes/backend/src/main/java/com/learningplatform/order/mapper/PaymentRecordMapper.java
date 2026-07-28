/* 文件职责：定义支付记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.PaymentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
/**
 * 定义支付记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface PaymentRecordMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, payment_no, order_id, provider, provider_transaction_no,
            amount, status, request_payload, response_payload, failure_reason,
            paid_at, created_at, updated_at
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM payment_record
            WHERE order_id = #{orderId}
            ORDER BY created_at ASC, id ASC
            """)
    /** 执行 findByOrderId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<PaymentRecord> findByOrderId(Long orderId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM payment_record
            WHERE order_id = #{orderId}
              AND status = 'SUCCESS'
            ORDER BY paid_at ASC, id ASC
            LIMIT 1
            """)
    /** 执行 findSuccessfulByOrderId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<PaymentRecord> findSuccessfulByOrderId(Long orderId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM payment_record
            WHERE payment_no = #{paymentNo}
            """)
    /** 执行 findByPaymentNo 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<PaymentRecord> findByPaymentNo(String paymentNo);

    @Insert("""
            INSERT INTO payment_record (
                payment_no, order_id, provider, provider_transaction_no,
                amount, status, request_payload, response_payload,
                failure_reason, paid_at
            ) VALUES (
                #{paymentNo}, #{orderId}, #{provider}, #{providerTransactionNo},
                #{amount}, #{status}, #{requestPayload}, #{responsePayload},
                #{failureReason}, #{paidAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(PaymentRecord payment);
}
