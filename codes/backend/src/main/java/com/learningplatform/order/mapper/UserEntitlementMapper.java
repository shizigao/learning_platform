/* 文件职责：定义用户权益的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;
import java.util.List;

@Mapper
/**
 * 定义用户权益的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface UserEntitlementMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, user_id, entitlement_type, resource_id, source_order_item_id,
            total_quantity, available_quantity, status, effective_at, expires_at,
            version, created_at, updated_at
            """;

    @Select("""
            SELECT COUNT(*) > 0
            FROM user_entitlement
            WHERE user_id = #{userId}
              AND entitlement_type = 'CONTENT_ACCESS'
              AND resource_id = #{contentId}
              AND status = 'ACTIVE'
              AND effective_at <= CURRENT_TIMESTAMP
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
            """)
    /** 判断是否满足Active学习资料访问权条件，不修改持久化状态。 */
    boolean hasActiveContentAccess(
            @Param("userId") Long userId,
            @Param("contentId") Long contentId
    );

    @Insert("""
            INSERT INTO user_entitlement (
                user_id, entitlement_type, resource_id, source_order_item_id,
                total_quantity, available_quantity, status, effective_at, expires_at, version
            ) VALUES (
                #{userId}, #{entitlementType}, #{resourceId}, #{sourceOrderItemId},
                #{totalQuantity}, #{availableQuantity}, #{status}, #{effectiveAt}, #{expiresAt}, #{version}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(UserEntitlement entitlement);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    /** 执行 findByUserId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    List<UserEntitlement> findByUserId(Long userId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE source_order_item_id = #{sourceOrderItemId}
            """)
    /** 执行 findBySourceOrderItemId 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<UserEntitlement> findBySourceOrderItemId(Long sourceOrderItemId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE user_id = #{userId}
              AND entitlement_type = #{entitlementType}
              AND status = 'ACTIVE'
              AND available_quantity > 0
              AND effective_at <= CURRENT_TIMESTAMP
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
            ORDER BY CASE WHEN expires_at IS NULL THEN 1 ELSE 0 END, expires_at ASC, id ASC
            LIMIT 1
            FOR UPDATE
            """)
    /** 执行 findAvailableQuotaForUpdate 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<UserEntitlement> findAvailableQuotaForUpdate(
            @Param("userId") Long userId,
            @Param("entitlementType") EntitlementType entitlementType
    );

    @Update("""
            UPDATE user_entitlement
            SET status = CASE
                    WHEN available_quantity = #{quantity} THEN 'EXHAUSTED'
                    ELSE status
                END,
                available_quantity = available_quantity - #{quantity},
                version = version + 1
            WHERE id = #{id}
              AND version = #{version}
              AND status = 'ACTIVE'
              AND available_quantity >= #{quantity}
            """)
    /** 执行 consume 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int consume(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("quantity") int quantity
    );

    @Update("""
            UPDATE user_entitlement
            SET total_quantity = total_quantity + #{quantity},
                available_quantity = available_quantity + #{quantity},
                status = 'ACTIVE',
                version = version + 1
            WHERE id = #{id}
              AND user_id = #{userId}
              AND entitlement_type = #{entitlementType}
              AND entitlement_type IN (
                  'AI_QUOTA', 'EXAM_QUOTA',
                  'EXAM_OVERALL_AI_QUOTA', 'EXAM_PERSONAL_AI_QUOTA'
              )
              AND status IN ('ACTIVE', 'EXHAUSTED')
              AND effective_at <= CURRENT_TIMESTAMP
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
              AND total_quantity <= 2147483647 - #{quantity}
              AND available_quantity <= 2147483647 - #{quantity}
            """)
    /** 执行 increaseQuota 对应的数据库操作；写操作返回受影响行数供服务层判断状态。 */
    int increaseQuota(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("entitlementType") EntitlementType entitlementType,
            @Param("quantity") int quantity
    );

    @Select("""
            SELECT COALESCE(SUM(available_quantity), 0)
            FROM user_entitlement
            WHERE user_id = #{userId}
              AND entitlement_type = #{entitlementType}
              AND status = 'ACTIVE'
              AND effective_at <= CURRENT_TIMESTAMP
              AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP)
            """)
    /** 执行 sumAvailableQuota 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    int sumAvailableQuota(
            @Param("userId") Long userId,
            @Param("entitlementType") EntitlementType entitlementType
    );

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    /** 执行 findByIdAndUser 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<UserEntitlement> findByIdAndUser(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}
