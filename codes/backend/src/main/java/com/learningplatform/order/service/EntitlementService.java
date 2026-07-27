package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderItem;
import com.learningplatform.order.domain.OrderStatus;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.domain.UserEntitlement;
import com.learningplatform.order.dto.EntitlementBalancesResponse;
import com.learningplatform.order.dto.EntitlementResponse;
import com.learningplatform.order.mapper.OrderItemMapper;
import com.learningplatform.order.mapper.OrderMapper;
import com.learningplatform.order.mapper.UserEntitlementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 用户权益的查询、发放与并发扣减服务。
 *
 * <p>资料访问权是资源型权益，AI/考试次数是配额型权益；两者字段约束不同。
 * 支付发放以订单项为幂等键，配额消费按最早可用批次加锁并使用版本号更新，
 * 防止并发请求把余额扣成负数。</p>
 */
@Service
public class EntitlementService {
    private final UserEntitlementMapper entitlementMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    public EntitlementService(
            UserEntitlementMapper entitlementMapper,
            OrderMapper orderMapper,
            OrderItemMapper itemMapper
    ) {
        this.entitlementMapper = entitlementMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
    }

    /** 列出用户全部权益批次，包含已用完或失效批次供历史展示。 */
    public List<EntitlementResponse> list(Long userId) {
        return entitlementMapper.findByUserId(userId).stream()
                .map(EntitlementResponse::from)
                .toList();
    }

    /** 聚合前端常用的四种次数余额。 */
    public EntitlementBalancesResponse balances(Long userId) {
        return new EntitlementBalancesResponse(
                availableQuota(userId, EntitlementType.AI_QUOTA),
                availableQuota(userId, EntitlementType.EXAM_QUOTA),
                availableQuota(userId, EntitlementType.EXAM_OVERALL_AI_QUOTA),
                availableQuota(userId, EntitlementType.EXAM_PERSONAL_AI_QUOTA)
        );
    }

    /** 判断用户是否拥有当前有效的指定资料访问权。 */
    public boolean hasActiveContentAccess(Long userId, Long contentId) {
        return userId != null
                && contentId != null
                && entitlementMapper.hasActiveContentAccess(userId, contentId);
    }

    /** 汇总指定配额类型所有有效批次的可用次数。 */
    public int availableQuota(Long userId, EntitlementType entitlementType) {
        requireQuotaType(entitlementType);
        return entitlementMapper.sumAvailableQuota(userId, entitlementType);
    }

    /** 管理端向指定权益批次追加次数，并保留原批次审计关系。 */
    @Transactional
    public EntitlementResponse increaseQuota(
            Long entitlementId,
            Long userId,
            EntitlementType entitlementType,
            int quantity
    ) {
        requireQuotaType(entitlementType);
        requirePositiveQuantity(quantity);
        if (entitlementMapper.increaseQuota(
                entitlementId,
                userId,
                entitlementType,
                quantity
        ) != 1) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "权益不存在、已失效或数量超过允许上限"
            );
        }
        return EntitlementResponse.from(
                entitlementMapper.findByIdAndUser(entitlementId, userId)
                        .orElseThrow(() -> new BusinessException(
                                ErrorCode.INTERNAL_ERROR,
                                "增加权益后无法读取权益记录"
                        ))
        );
    }

    /**
     * 原子消费配额。
     * 单次消费可跨越多个权益批次，任一阶段失败会由事务回滚全部扣减。
     */
    @Transactional
    public void consumeQuota(
            Long userId,
            EntitlementType entitlementType,
            int quantity
    ) {
        requireQuotaType(entitlementType);
        requirePositiveQuantity(quantity);
        int remaining = quantity;
        while (remaining > 0) {
            UserEntitlement entitlement = entitlementMapper
                    .findAvailableQuotaForUpdate(userId, entitlementType)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.FORBIDDEN,
                            "权益额度不足"
                    ));
            int consumed = Math.min(
                    remaining,
                    entitlement.getAvailableQuantity()
            );
            if (entitlementMapper.consume(
                    entitlement.getId(),
                    entitlement.getVersion(),
                    consumed
            ) != 1) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "权益额度发生变化，请重试"
                );
            }
            remaining -= consumed;
        }
    }

    /** 创建经过类型不变量校验的新权益，并拒绝重复订单项。 */
    @Transactional
    public EntitlementResponse create(UserEntitlement entitlement) {
        normalizeAndValidate(entitlement);
        if (entitlement.getSourceOrderItemId() != null
                && entitlementMapper.findBySourceOrderItemId(
                        entitlement.getSourceOrderItemId()
                ).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单项权益已经发放");
        }
        if (entitlementMapper.insert(entitlement) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建用户权益失败");
        }
        return EntitlementResponse.from(entitlement);
    }

    /** 为已支付订单的每个订单项幂等发放对应权益。 */
    @Transactional
    public List<EntitlementResponse> grantForPaidOrder(Long orderId, Long userId) {
        Order order = orderMapper.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "订单不存在"
                ));
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单尚未支付，不能发放权益");
        }
        return itemMapper.findByOrderId(orderId).stream()
                .map(item -> grantItem(order, item))
                .toList();
    }

    private EntitlementResponse grantItem(Order order, OrderItem item) {
        UserEntitlement expected = entitlementFor(order, item);
        return entitlementMapper.findBySourceOrderItemId(item.getId())
                .map(existing -> {
                    assertSameEntitlement(existing, expected);
                    return EntitlementResponse.from(existing);
                })
                .orElseGet(() -> create(expected));
    }

    private UserEntitlement entitlementFor(Order order, OrderItem item) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(order.getUserId());
        entitlement.setSourceOrderItemId(item.getId());
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setEffectiveAt(
                order.getPaidAt() == null
                        ? LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                        : order.getPaidAt()
        );
        entitlement.setVersion(0);

        ProductType productType = item.getProductTypeSnapshot();
        if (productType == ProductType.CONTENT) {
            entitlement.setEntitlementType(EntitlementType.CONTENT_ACCESS);
            entitlement.setResourceId(item.getResourceIdSnapshot());
            return entitlement;
        }
        int quantity = entitlementQuantity(item);
        entitlement.setEntitlementType(entitlementType(productType));
        entitlement.setTotalQuantity(quantity);
        entitlement.setAvailableQuantity(quantity);
        return entitlement;
    }

    private int entitlementQuantity(OrderItem item) {
        if (item.getEntitlementQuantity() == null
                || item.getEntitlementQuantity() <= 0
                || item.getQuantity() == null
                || item.getQuantity() <= 0) {
            throw invalid("订单项权益数量无效");
        }
        try {
            return Math.multiplyExact(
                    item.getEntitlementQuantity(),
                    item.getQuantity()
            );
        } catch (ArithmeticException exception) {
            throw invalid("订单项权益数量超过允许上限");
        }
    }

    private void assertSameEntitlement(
            UserEntitlement existing,
            UserEntitlement expected
    ) {
        if (!existing.getUserId().equals(expected.getUserId())
                || existing.getEntitlementType() != expected.getEntitlementType()
                || !java.util.Objects.equals(
                        existing.getResourceId(),
                        expected.getResourceId()
                )
                || !java.util.Objects.equals(
                        existing.getTotalQuantity(),
                        expected.getTotalQuantity()
                )) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "订单项已存在不一致的权益记录"
            );
        }
    }

    private void normalizeAndValidate(UserEntitlement entitlement) {
        if (entitlement.getUserId() == null || entitlement.getUserId() <= 0) {
            throw invalid("权益用户无效");
        }
        if (entitlement.getEntitlementType() == null) {
            throw invalid("权益类型不能为空");
        }
        if (entitlement.getEntitlementType() == EntitlementType.CONTENT_ACCESS) {
            if (entitlement.getResourceId() == null || entitlement.getResourceId() <= 0) {
                throw invalid("资料访问权必须关联有效资料");
            }
            if (entitlement.getTotalQuantity() != null
                    || entitlement.getAvailableQuantity() != null) {
                throw invalid("资料访问权不能配置次数");
            }
        } else {
            if (entitlement.getResourceId() != null) {
                throw invalid("次数权益不能关联资料");
            }
            if (entitlement.getTotalQuantity() == null
                    || entitlement.getTotalQuantity() <= 0
                    || entitlement.getAvailableQuantity() == null
                    || entitlement.getAvailableQuantity() < 0
                    || entitlement.getAvailableQuantity() > entitlement.getTotalQuantity()) {
                throw invalid("次数权益数量无效");
            }
        }
        if (entitlement.getStatus() == null) {
            entitlement.setStatus(EntitlementStatus.ACTIVE);
        }
        if (entitlement.getEffectiveAt() == null) {
            entitlement.setEffectiveAt(
                    LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
            );
        }
        if (entitlement.getVersion() == null) {
            entitlement.setVersion(0);
        }
    }

    private void requireQuotaType(EntitlementType entitlementType) {
        if (entitlementType != EntitlementType.AI_QUOTA
                && entitlementType != EntitlementType.EXAM_QUOTA
                && entitlementType != EntitlementType.EXAM_OVERALL_AI_QUOTA
                && entitlementType != EntitlementType.EXAM_PERSONAL_AI_QUOTA) {
            throw invalid("该权益类型不支持次数操作");
        }
    }

    private EntitlementType entitlementType(ProductType productType) {
        return switch (productType) {
            case AI_PACKAGE -> EntitlementType.AI_QUOTA;
            case EXAM_PACKAGE -> EntitlementType.EXAM_QUOTA;
            case EXAM_OVERALL_AI_PACKAGE -> EntitlementType.EXAM_OVERALL_AI_QUOTA;
            case EXAM_PERSONAL_AI_PACKAGE -> EntitlementType.EXAM_PERSONAL_AI_QUOTA;
            case CONTENT -> throw invalid("资料商品不使用次数权益");
        };
    }

    private void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw invalid("权益操作数量必须大于0");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
