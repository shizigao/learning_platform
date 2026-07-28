/* 文件职责：定义订单Item的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
/**
 * 定义订单Item的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface OrderItemMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, order_id, product_id, product_code_snapshot, product_type_snapshot,
            product_name_snapshot, resource_id_snapshot, unit_price, quantity,
            entitlement_quantity, subtotal_amount, created_at
            """;

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM order_item
            WHERE order_id = #{orderId}
            ORDER BY id ASC
            """)
    /** 执行 findByOrderId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<OrderItem> findByOrderId(Long orderId);

    @Insert("""
            INSERT INTO order_item (
                order_id, product_id, product_code_snapshot, product_type_snapshot,
                product_name_snapshot, resource_id_snapshot, unit_price, quantity,
                entitlement_quantity, subtotal_amount
            ) VALUES (
                #{orderId}, #{productId}, #{productCodeSnapshot}, #{productTypeSnapshot},
                #{productNameSnapshot}, #{resourceIdSnapshot}, #{unitPrice}, #{quantity},
                #{entitlementQuantity}, #{subtotalAmount}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(OrderItem item);
}
