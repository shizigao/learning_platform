package com.learningplatform.order.mapper;

import com.learningplatform.order.domain.Product;
import com.learningplatform.order.domain.ProductType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProductMapper {
    String COLUMNS = """
            id, product_code, product_type, name, description, resource_id,
            quantity, price, status, sort_order, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM product WHERE id = #{id} AND deleted = 0")
    Optional<Product> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM product
            WHERE product_code = #{productCode} AND deleted = 0
            """)
    Optional<Product> findByCode(String productCode);

    @Select("""
            <script>
            SELECT
            """ + COLUMNS + """
            FROM product
            WHERE status = 'ACTIVE' AND deleted = 0
            <if test="productType != null">
              AND product_type = #{productType}
            </if>
            ORDER BY sort_order ASC, id ASC
            </script>
            """)
    List<Product> findActive(@Param("productType") ProductType productType);

    @Insert("""
            INSERT INTO product (
                product_code, product_type, name, description, resource_id,
                quantity, price, status, sort_order
            ) VALUES (
                #{productCode}, #{productType}, #{name}, #{description}, #{resourceId},
                #{quantity}, #{price}, #{status}, #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Update("""
            UPDATE product
            SET name = #{name}, description = #{description}, resource_id = #{resourceId},
                quantity = #{quantity}, price = #{price}, status = #{status},
                sort_order = #{sortOrder}
            WHERE id = #{id} AND deleted = 0
            """)
    int update(Product product);

    @Update("UPDATE product SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDelete(Long id);
}
