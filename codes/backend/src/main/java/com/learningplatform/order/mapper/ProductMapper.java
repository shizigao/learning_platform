/* 文件职责：定义商品的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：MyBatis 持久化层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 定义商品的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。
 *
 * <p>职责边界：只表达数据库读写语义，不在 SQL 映射层做权限和业务决策。</p>
 */
public interface ProductMapper {
    /** 复用学习资料查询列，保证不同查询返回一致字段集合。 */
    String COLUMNS = """
            id, product_code, product_type, name, description, resource_id,
            quantity, price, status, sort_order, created_at, updated_at, deleted
            """;

    @Select("SELECT " + COLUMNS + " FROM product WHERE id = #{id} AND deleted = 0")
    /** 执行 findById 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
    Optional<Product> findById(Long id);

    @Select("""
            SELECT
            """ + COLUMNS + """
            FROM product
            WHERE product_code = #{productCode} AND deleted = 0
            """)
    /** 执行 findByCode 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 执行 findActive 数据库查询，返回领域对象、聚合值或是否存在的判断结果。 */
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
    /** 插入新记录，并返回受影响行数；配置生成主键时同时回填实体 ID。 */
    int insert(Product product);

    @Update("""
            UPDATE product
            SET name = #{name}, description = #{description}, resource_id = #{resourceId},
                quantity = #{quantity}, price = #{price}, status = #{status},
                sort_order = #{sortOrder}
            WHERE id = #{id} AND deleted = 0
            """)
    /** 执行 update 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int update(Product product);

    @Update("UPDATE product SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    /** 执行 softDelete 条件写入并返回受影响行数，服务层据此识别状态冲突或并发修改。 */
    int softDelete(Long id);
}
