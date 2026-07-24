package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface OrderMapper {
    String COLUMNS = """
            id, order_no, user_id, status, total_amount, payable_amount,
            paid_amount, payment_method, remark, expires_at, paid_at,
            cancelled_at, version, created_at, updated_at
            """;

    @Select("SELECT " + COLUMNS + " FROM orders WHERE id = #{id}")
    Optional<Order> findById(Long id);

    @Select("SELECT " + COLUMNS + " FROM orders WHERE id = #{id} FOR UPDATE")
    Optional<Order> findByIdForUpdate(Long id);

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM orders
            WHERE user_id = #{userId}
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Order> findByUser(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM orders
            WHERE user_id = #{userId}
            <if test="status != null">
              AND status = #{status}
            </if>
            </script>
            """)
    long countByUser(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM orders
            WHERE 1 = 1
            <if test="orderNo != null and orderNo != ''">
              AND order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="userId != null">
              AND user_id = #{userId}
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Order> findAll(
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM orders
            WHERE 1 = 1
            <if test="orderNo != null and orderNo != ''">
              AND order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="userId != null">
              AND user_id = #{userId}
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            </script>
            """)
    long countAll(
            @Param("orderNo") String orderNo,
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );

    @Insert("""
            INSERT INTO orders (
                order_no, user_id, status, total_amount, payable_amount,
                remark, expires_at, version
            ) VALUES (
                #{orderNo}, #{userId}, #{status}, #{totalAmount}, #{payableAmount},
                #{remark}, #{expiresAt}, #{version}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Order order);

    @Update("""
            UPDATE orders
            SET status = 'CANCELLED', cancelled_at = #{cancelledAt}, version = version + 1
            WHERE id = #{id} AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT' AND version = #{version}
            """)
    int cancel(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("version") Integer version,
            @Param("cancelledAt") LocalDateTime cancelledAt
    );

    @Update("""
            UPDATE orders
            SET status = 'PAID', paid_amount = #{paidAmount},
                payment_method = 'MOCK', paid_at = #{paidAt}, version = version + 1
            WHERE id = #{id} AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT' AND version = #{version}
            """)
    int markMockPaid(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("version") Integer version,
            @Param("paidAmount") BigDecimal paidAmount,
            @Param("paidAt") LocalDateTime paidAt
    );
}
