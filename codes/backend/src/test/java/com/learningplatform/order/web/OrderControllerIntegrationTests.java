package com.learningplatform.order.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.domain.UserEntitlement;
import com.learningplatform.order.dto.EntitlementResponse;
import com.learningplatform.order.dto.ProductResponse;
import com.learningplatform.order.dto.ProductWriteRequest;
import com.learningplatform.order.service.EntitlementService;
import com.learningplatform.order.service.ProductService;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/user-schema.sql", "/sql/order-schema.sql"})
class OrderControllerIntegrationTests {
    private static final String PASSWORD = "Password123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductService productService;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User buyer;
    private String buyerToken;
    private String otherToken;
    private String adminToken;
    private long contentProductId;
    private long aiProductId;
    private long examProductId;

    @BeforeEach
    void setUp() throws Exception {
        buyer = createUser("order_buyer", RoleCode.USER);
        createUser("other_order_buyer", RoleCode.USER);
        createUser("order_admin", RoleCode.ADMIN);
        buyerToken = login("order_buyer");
        otherToken = login("other_order_buyer");
        adminToken = login("order_admin");
        contentProductId = createProduct(
                "CONTENT_DATABASE",
                ProductType.CONTENT,
                100L,
                null,
                "29.90"
        ).id();
        aiProductId = createProduct(
                "AI_PACKAGE_10",
                ProductType.AI_PACKAGE,
                null,
                10,
                "9.90"
        ).id();
        examProductId = createProduct(
                "EXAM_PACKAGE_5",
                ProductType.EXAM_PACKAGE,
                null,
                5,
                "19.90"
        ).id();
    }

    @Test
    void listsThreeProductModelsAndCreatesServerPricedSnapshotOrder() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(get("/api/products")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .param("productType", "AI_PACKAGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].quantity").value(10));

        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"productId": %d, "quantity": 2, "unitPrice": 0.01},
                                    {"productId": %d, "quantity": 1, "unitPrice": 0.01}
                                  ],
                                  "remark": " 阶段 G 测试 ",
                                  "totalAmount": 0.02,
                                  "payableAmount": 0.02
                                }
                                """.formatted(aiProductId, examProductId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(39.70))
                .andExpect(jsonPath("$.data.payableAmount").value(39.70))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].productCode").value("AI_PACKAGE_10"))
                .andExpect(jsonPath("$.data.items[0].entitlementQuantity").value(10))
                .andExpect(jsonPath("$.data.items[0].subtotalAmount").value(19.80))
                .andExpect(jsonPath("$.data.paymentNotice")
                        .value("当前仅为模拟支付，不会产生真实资金交易"))
                .andReturn();
        long orderId = responseData(created).path("id").asLong();

        jdbcTemplate.update("UPDATE product SET price = 99.90 WHERE id = ?", aiProductId);
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(39.70))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(9.90));
        mockMvc.perform(get("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(orderId));
    }

    @Test
    void cancelsOnlyPendingOwnedOrderAndHidesItFromOtherUsers() throws Exception {
        long orderId = createSingleItemOrder(aiProductId);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header(AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("订单不存在"));
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());
        mockMvc.perform(post("/api/orders/{id}/mock-pay", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("订单不是待支付状态"));
    }

    @Test
    void idempotentlyPaysAndIssuesAllThreeEntitlementTypes() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"productId": %d, "quantity": 1},
                                    {"productId": %d, "quantity": 2},
                                    {"productId": %d, "quantity": 3}
                                  ]
                                }
                                """.formatted(
                                        contentProductId,
                                        aiProductId,
                                        examProductId
                                )))
                .andExpect(status().isOk())
                .andReturn();
        long orderId = responseData(created).path("id").asLong();

        MvcResult firstPayment = mockMvc.perform(post("/api/orders/{id}/mock-pay", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("PAID"))
                .andExpect(jsonPath("$.data.order.paidAmount").value(109.40))
                .andExpect(jsonPath("$.data.order.paymentMethod").value("MOCK"))
                .andExpect(jsonPath("$.data.payment.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.payment.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.notice")
                        .value("模拟支付成功：本次操作不会产生真实资金交易"))
                .andReturn();
        String paymentNo = responseData(firstPayment)
                .path("payment")
                .path("paymentNo")
                .asText();

        mockMvc.perform(post("/api/orders/{id}/mock-pay", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order.status").value("PAID"))
                .andExpect(jsonPath("$.data.payment.paymentNo").value(paymentNo));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE order_id = ? AND status = 'SUCCESS'",
                Integer.class,
                orderId
        )).isEqualTo(1);
        mockMvc.perform(get("/api/entitlements")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath(
                        "$.data[?(@.entitlementType == 'CONTENT_ACCESS')].resourceId"
                ).value(100))
                .andExpect(jsonPath(
                        "$.data[?(@.entitlementType == 'AI_QUOTA')].totalQuantity"
                ).value(20))
                .andExpect(jsonPath(
                        "$.data[?(@.entitlementType == 'AI_QUOTA')].availableQuantity"
                ).value(20))
                .andExpect(jsonPath(
                        "$.data[?(@.entitlementType == 'EXAM_QUOTA')].totalQuantity"
                ).value(15));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_entitlement WHERE user_id = ?",
                Integer.class,
                buyer.getId()
        )).isEqualTo(3);
    }

    @Test
    void rollsBackPaymentWhenEntitlementIssuingFails() throws Exception {
        long orderId = createSingleItemOrder(aiProductId);
        jdbcTemplate.update(
                "UPDATE order_item SET entitlement_quantity = NULL WHERE order_id = ?",
                orderId
        );

        mockMvc.perform(post("/api/orders/{id}/mock-pay", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单项权益数量无效"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?",
                String.class,
                orderId
        )).isEqualTo("PENDING_PAYMENT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_record WHERE order_id = ?",
                Integer.class,
                orderId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_entitlement WHERE user_id = ?",
                Integer.class,
                buyer.getId()
        )).isZero();
    }

    @Test
    void rejectsExpiredPaymentAndInvalidOrderItems() throws Exception {
        long orderId = createSingleItemOrder(aiProductId);
        jdbcTemplate.update(
                "UPDATE orders SET expires_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)),
                orderId
        );
        mockMvc.perform(post("/api/orders/{id}/mock-pay", orderId)
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("订单已超过支付期限"));

        mockMvc.perform(post("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"productId": %d, "quantity": 1},
                                    {"productId": %d, "quantity": 1}
                                  ]
                                }
                                """.formatted(aiProductId, aiProductId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("订单商品不能重复"));

        mockMvc.perform(post("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":2}]}
                                """.formatted(contentProductId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("付费资料商品的购买数量只能为1"));
    }

    @Test
    void exposesValidatedUserEntitlementBusinessModel() throws Exception {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(buyer.getId());
        entitlement.setEntitlementType(EntitlementType.EXAM_QUOTA);
        entitlement.setTotalQuantity(5);
        entitlement.setAvailableQuantity(5);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlementService.create(entitlement);

        mockMvc.perform(get("/api/entitlements")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entitlementType").value("EXAM_QUOTA"))
                .andExpect(jsonPath("$.data[0].totalQuantity").value(5))
                .andExpect(jsonPath("$.data[0].availableQuantity").value(5));
    }

    @Test
    void atomicallyIncreasesConsumesAndSummarizesQuota() throws Exception {
        EntitlementResponse created = entitlementService.create(
                quotaEntitlement(EntitlementType.AI_QUOTA, 2)
        );

        EntitlementResponse increased = entitlementService.increaseQuota(
                created.id(),
                buyer.getId(),
                EntitlementType.AI_QUOTA,
                3
        );
        assertThat(increased.totalQuantity()).isEqualTo(5);
        assertThat(increased.availableQuantity()).isEqualTo(5);

        entitlementService.consumeQuota(
                buyer.getId(),
                EntitlementType.AI_QUOTA,
                4
        );
        assertThat(entitlementService.availableQuota(
                buyer.getId(),
                EntitlementType.AI_QUOTA
        )).isEqualTo(1);

        assertThatThrownBy(() -> entitlementService.consumeQuota(
                buyer.getId(),
                EntitlementType.AI_QUOTA,
                2
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
        assertThat(entitlementService.availableQuota(
                buyer.getId(),
                EntitlementType.AI_QUOTA
        )).isEqualTo(1);

        mockMvc.perform(get("/api/entitlements/balances")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiQuota").value(1))
                .andExpect(jsonPath("$.data.examQuota").value(0));
    }

    @Test
    void concurrentConsumptionNeverMakesQuotaNegative() throws Exception {
        entitlementService.create(quotaEntitlement(EntitlementType.EXAM_QUOTA, 1));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ErrorCode>> results = List.of(
                    executor.submit(() -> consumeAfterStart(start)),
                    executor.submit(() -> consumeAfterStart(start))
            );
            start.countDown();

            List<ErrorCode> errorCodes = results.stream()
                    .map(result -> {
                        try {
                            return result.get(5, TimeUnit.SECONDS);
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
            assertThat(errorCodes).containsExactlyInAnyOrder(null, ErrorCode.FORBIDDEN);
        } finally {
            executor.shutdownNow();
        }

        assertThat(entitlementService.availableQuota(
                buyer.getId(),
                EntitlementType.EXAM_QUOTA
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT MIN(available_quantity) FROM user_entitlement WHERE user_id = ?",
                Integer.class,
                buyer.getId()
        )).isZero();
    }

    @Test
    void adminCanSearchAllOrdersWhileRegularUserIsForbidden() throws Exception {
        long orderId = createSingleItemOrder(aiProductId);

        mockMvc.perform(get("/api/admin/orders")
                        .header(AUTHORIZATION, bearer(buyerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/orders")
                        .header(AUTHORIZATION, bearer(adminToken))
                        .param("orderNo", "ORD")
                        .param("userId", buyer.getId().toString())
                        .param("status", "PENDING_PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(orderId))
                .andExpect(jsonPath("$.data.items[0].userId").value(buyer.getId()));
        mockMvc.perform(get("/api/admin/orders/{id}", orderId)
                        .header(AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    private ErrorCode consumeAfterStart(CountDownLatch start) throws InterruptedException {
        start.await();
        try {
            entitlementService.consumeQuota(
                    buyer.getId(),
                    EntitlementType.EXAM_QUOTA,
                    1
            );
            return null;
        } catch (BusinessException exception) {
            return exception.getErrorCode();
        }
    }

    private UserEntitlement quotaEntitlement(
            EntitlementType entitlementType,
            int quantity
    ) {
        UserEntitlement entitlement = new UserEntitlement();
        entitlement.setUserId(buyer.getId());
        entitlement.setEntitlementType(entitlementType);
        entitlement.setTotalQuantity(quantity);
        entitlement.setAvailableQuantity(quantity);
        entitlement.setStatus(EntitlementStatus.ACTIVE);
        entitlement.setEffectiveAt(LocalDateTime.now().minusMinutes(1));
        entitlement.setVersion(0);
        return entitlement;
    }

    private long createSingleItemOrder(long productId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .header(AUTHORIZATION, bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":%d,"quantity":1}]}
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private ProductResponse createProduct(
            String code,
            ProductType type,
            Long resourceId,
            Integer quantity,
            String price
    ) {
        return productService.create(new ProductWriteRequest(
                code,
                type,
                code,
                "测试商品",
                resourceId,
                quantity,
                new BigDecimal(price),
                null,
                0
        ));
    }

    private User createUser(String username, RoleCode roleCode) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setNickname(username);
        user.setStatus(UserStatus.ACTIVE);
        userService.create(user);
        roleService.assignRole(user.getId(), roleCode, null);
        return user;
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return responseData(result).path("accessToken").asText();
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
