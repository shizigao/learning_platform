/* 文件职责：实现商品业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.Product;
import com.learningplatform.order.domain.ProductStatus;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.dto.ProductResponse;
import com.learningplatform.order.dto.ProductWriteRequest;
import com.learningplatform.order.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
/**
 * 实现商品业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ProductService {
    /** 访问商品持久化数据。 */
    private final ProductMapper productMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /** 查询Active相关数据；只返回当前调用方有权查看的结果。 */
    public List<ProductResponse> listActive(ProductType productType) {
        return productMapper.findActive(productType).stream()
                .map(ProductResponse::from)
                .toList();
    }

    /** 返回Required。 */
    public Product getRequired(Long productId) {
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
    }

    /** 返回Purchasable。 */
    public Product getPurchasable(Long productId) {
        Product product = getRequired(productId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已下架");
        }
        validateModel(product);
        return product;
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public ProductResponse create(ProductWriteRequest request) {
        String productCode = request.productCode().trim().toUpperCase(Locale.ROOT);
        if (productMapper.findByCode(productCode).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品编码已存在");
        }
        Product product = new Product();
        product.setProductCode(productCode);
        product.setProductType(request.productType());
        product.setName(request.name().trim());
        product.setDescription(normalize(request.description()));
        product.setResourceId(request.resourceId());
        product.setQuantity(request.quantity());
        product.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
        product.setStatus(request.status() == null ? ProductStatus.ACTIVE : request.status());
        product.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        validateModel(product);
        if (productMapper.insert(product) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建商品失败");
        }
        return ProductResponse.from(product);
    }

    /** 校验Model及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void validateModel(Product product) {
        if (product.getProductType() == ProductType.CONTENT) {
            if (product.getResourceId() == null || product.getResourceId() <= 0) {
                throw invalid("付费资料商品必须关联有效资料");
            }
            if (product.getQuantity() != null) {
                throw invalid("付费资料商品不能配置次数");
            }
            return;
        }
        if (product.getResourceId() != null) {
            throw invalid("次数包商品不能关联资料");
        }
        if (product.getQuantity() == null || product.getQuantity() <= 0) {
            throw invalid("次数包商品必须配置大于0的次数");
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 invalid 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
