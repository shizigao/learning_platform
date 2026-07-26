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
public interface UserEntitlementMapper {
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
    int insert(UserEntitlement entitlement);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<UserEntitlement> findByUserId(Long userId);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM user_entitlement
            WHERE source_order_item_id = #{sourceOrderItemId}
            """)
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
    Optional<UserEntitlement> findByIdAndUser(
            @Param("id") Long id,
            @Param("userId") Long userId
    );
}
