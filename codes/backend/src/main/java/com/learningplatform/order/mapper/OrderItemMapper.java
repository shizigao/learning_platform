package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper {
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
    int insert(OrderItem item);
}
