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
public class ProductService {
    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public List<ProductResponse> listActive(ProductType productType) {
        return productMapper.findActive(productType).stream()
                .map(ProductResponse::from)
                .toList();
    }

    public Product getRequired(Long productId) {
        return productMapper.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "商品不存在"));
    }

    public Product getPurchasable(Long productId) {
        Product product = getRequired(productId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CONFLICT, "商品已下架");
        }
        validateModel(product);
        return product;
    }

    @Transactional
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
