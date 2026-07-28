/* 文件职责：定义订单的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义订单的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface OrderMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, order_no, user_id, status, total_amount, payable_amount,
            paid_amount, payment_method, remark, expires_at, paid_at,
            cancelled_at, version, created_at, updated_at
            """;

    @Select("SELECT " + COLUMNS + " FROM orders WHERE id = #{id}")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<Order> findById(Long id);

    @Select("SELECT " + COLUMNS + " FROM orders WHERE id = #{id} FOR UPDATE")
    /** 执行 findByIdForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findByUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 countByUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    long countByUser(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );

    @Select("""
            SELECT COUNT(*) > 0
            FROM orders o
            INNER JOIN order_item oi ON oi.order_id = o.id
            WHERE o.user_id = #{userId}
              AND oi.product_type_snapshot = 'CONTENT'
              AND oi.resource_id_snapshot = #{resourceId}
              AND (#{excludedOrderId} IS NULL OR o.id <> #{excludedOrderId})
              AND (
                    o.status = 'PAID'
                    OR (
                        o.status = 'PENDING_PAYMENT'
                        AND (o.expires_at IS NULL OR o.expires_at > CURRENT_TIMESTAMP)
                    )
                  )
            """)
    /** 执行 existsBlockingContentOrder 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    boolean existsBlockingContentOrder(
            @Param("userId") Long userId,
            @Param("resourceId") Long resourceId,
            @Param("excludedOrderId") Long excludedOrderId
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
    /** 执行 findAll 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 countAll 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(Order order);

    @Update("""
            UPDATE orders
            SET status = 'CANCELLED', cancelled_at = #{cancelledAt}, version = version + 1
            WHERE id = #{id} AND user_id = #{userId}
              AND status = 'PENDING_PAYMENT' AND version = #{version}
            """)
    /** 判断是否满足cel条件，不修改持久化状态。 */
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
    /** 执行 markMockPaid 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int markMockPaid(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("version") Integer version,
            @Param("paidAmount") BigDecimal paidAmount,
            @Param("paidAt") LocalDateTime paidAt
    );
}
