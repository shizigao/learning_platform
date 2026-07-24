package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.PaymentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PaymentRecordMapper {
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
    Optional<PaymentRecord> findSuccessfulByOrderId(Long orderId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM payment_record
            WHERE payment_no = #{paymentNo}
            """)
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
    int insert(PaymentRecord payment);
}
