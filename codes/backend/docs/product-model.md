# 商品模型

阶段 G 使用 `product` 表和 `Product` 领域对象统一表示三类商品，通过
`product_type` 区分权益目标：

| 商品类型 | `resource_id` | `quantity` | 支付后权益 |
| --- | --- | --- | --- |
| `CONTENT` | 必须为有效资料 ID | 必须为空 | `CONTENT_ACCESS` |
| `AI_PACKAGE` | 必须为空 | 必须为正整数 | `AI_QUOTA` |
| `EXAM_PACKAGE` | 必须为空 | 必须为正整数 | `EXAM_QUOTA` |

所有商品使用唯一 `product_code`；业务层统一转换为大写。价格使用两位小数，
状态为 `ACTIVE` 或 `INACTIVE`。只有状态为 `ACTIVE` 且结构符合上表约束的商品
可以进入后续订单流程。

`ProductMapper` 提供按 ID、编码及类型读取商品，以及创建、更新和逻辑删除能力。
`ProductService` 负责三类模型约束、编码和文本标准化，并为后续创建订单提供
`getPurchasable` 入口。

初始化脚本 `database/002_seed_data.sql` 已包含 AI 次数包和考试发布次数包。
付费资料商品必须关联实际资料，因此应在资料创建完成后通过后续管理功能或
初始化 SQL 创建，不能使用无效占位资料 ID。
