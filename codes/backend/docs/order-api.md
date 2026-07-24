# 订单与模拟支付 API

## 适用范围

本文档对应阶段 G 的 OP-128～OP-136，覆盖商品查询、订单创建与查询、未支付订单取消、幂等模拟支付、支付后权益发放、权益余额、正式业务接入、管理员订单查询与前端操作页面。

所有 `/api/orders/**` 与 `/api/entitlements` 接口都需要登录。订单接口只返回当前用户自己的订单；访问其他用户的订单统一按“订单不存在”处理，避免泄露订单信息。

## 数据与状态

- 商品类型：`CONTENT`、`AI_PACKAGE`、`EXAM_PACKAGE`
- 订单状态：`PENDING_PAYMENT`、`PAID`、`CANCELLED`、`CLOSED`、`REFUNDED`
- 支付渠道：当前仅支持 `MOCK`
- 支付状态：`PENDING`、`SUCCESS`、`FAILED`、`CLOSED`、`REFUNDED`
- 权益类型：`CONTENT_ACCESS`、`AI_QUOTA`、`EXAM_QUOTA`

订单创建时由服务端读取商品价格并计算总额，不接受客户端传入金额。订单项保存商品编码、类型、名称、关联资料、单价和权益数量快照，因此后续修改商品不会改变已创建订单。

待支付订单的默认支付期限为 30 分钟。只有 `PENDING_PAYMENT` 订单可以取消或执行模拟支付。

## 商品接口

### 查询在售商品

```http
GET /api/products
GET /api/products?productType=AI_PACKAGE
```

### 查询商品详情

```http
GET /api/products/{productId}
```

下架、不存在或配置不合法的商品不会作为可购买商品返回。

## 订单接口

### 创建订单

```http
POST /api/orders
Content-Type: application/json
Authorization: Bearer <token>

{
  "items": [
    {
      "productId": 1,
      "quantity": 1
    }
  ],
  "remark": "测试订单"
}
```

约束：

- 一张订单包含 1～20 种商品。
- 同一个商品不能重复出现。
- 单项数量为 1～99。
- `CONTENT` 商品数量必须为 1。
- 同一资料即使对应不同商品编码，也不能在一张订单中重复购买。
- 用户已有该资料的有效访问权，或已有该资料的有效待支付/已支付订单时，不能再次创建订单；已取消、已关闭或已过期的待支付订单不阻止重新购买。
- 总金额上限为 `99999999.99`。

成功后订单状态为 `PENDING_PAYMENT`，响应中包含订单项快照和明确的模拟支付提示。

付费资料的防重复判断以“用户 ID + 资料 ID”为准，而不是商品 ID。订单创建通过用户行锁串行检查，避免并发点击生成两张有效订单；支付时会再次检查资料访问权，以阻止历史遗留的重复待支付订单继续付款和重复发放权益。

### 查询我的订单

```http
GET /api/orders?pageNumber=1&pageSize=20
GET /api/orders?status=PENDING_PAYMENT&pageNumber=1&pageSize=20
```

### 查询我的订单详情

```http
GET /api/orders/{orderId}
```

详情包含订单项和支付记录。其他用户访问时返回 404。

### 管理员查询订单

```http
GET /api/admin/orders?pageNumber=1&pageSize=20
GET /api/admin/orders?orderNo=ORD&userId=1&status=PAID&pageNumber=1&pageSize=20
GET /api/admin/orders/{orderId}
```

仅 `ADMIN` 角色可以访问。列表支持按订单号、用户 ID 和订单状态组合筛选，详情包含订单项快照与支付记录；普通用户和发布者访问时返回 403。

### 取消未支付订单

```http
POST /api/orders/{orderId}/cancel
```

只有当前用户自己的 `PENDING_PAYMENT` 订单可以取消。成功后状态变为 `CANCELLED`，已支付或已取消订单再次取消会返回 409。

### 模拟支付

```http
POST /api/orders/{orderId}/mock-pay
```

成功后：

- 新增一条渠道为 `MOCK`、状态为 `SUCCESS` 的支付记录。
- 订单状态变为 `PAID`，记录支付金额、支付方式与支付时间。
- 按订单项商品类型发放资料访问权、AI 次数或考试发布次数。
- 响应明确返回“不会产生真实资金交易”的提示。

该接口仅用于本地开发和验收，不会连接真实支付渠道，也不会产生真实资金交易。

支付接口具有幂等性：

- 同一订单的处理使用数据库行锁串行化。
- 订单状态更新、支付记录写入和权益发放位于同一个事务；任一环节失败会整体回滚。
- 重复请求已支付订单会返回原成功支付记录，不会创建第二条成功支付记录。
- 每个订单项通过唯一 `source_order_item_id` 只发放一份权益；重复处理会返回已有权益。
- 订单号、支付号和模拟渠道流水号同时具有数据库唯一约束。

## 权益查询

```http
GET /api/entitlements
```

仅返回当前用户的权益明细。

```http
GET /api/entitlements/balances
```

返回当前用户的 AI 和考试发布可用次数汇总：

```json
{
  "code": 0,
  "data": {
    "aiQuota": 10,
    "examQuota": 5
  }
}
```

权益业务层校验以下数据规则：

- `CONTENT_ACCESS` 必须关联资料，不设置次数。
- `AI_QUOTA`、`EXAM_QUOTA` 不关联资料，必须设置有效的总次数和可用次数。
- 同一个订单项不能重复创建权益。

支付成功后的发放规则：

- `CONTENT` 商品发放一份 `CONTENT_ACCESS`，关联商品快照中的资料 ID。
- `AI_PACKAGE` 商品发放 `AI_QUOTA`。
- `EXAM_PACKAGE` 商品发放 `EXAM_QUOTA`。
- 次数包实际发放量为“商品每件次数 × 购买件数”，总次数和可用次数初始相同。

## 权益额度并发规则

- 只允许对 `AI_QUOTA` 和 `EXAM_QUOTA` 执行次数增加或扣减。
- 增加操作使用单条条件更新，同时增加总次数和可用次数，并防止整数溢出。
- 扣减操作按“最早过期优先、无过期时间最后”的顺序锁定权益记录。
- 一次请求可跨多条权益扣减；总余额不足时整个事务回滚，不会发生部分扣减。
- SQL 同时校验状态、版本和 `available_quantity >= quantity`，余额不会被扣成负数。
- 额度耗尽后权益状态自动变为 `EXHAUSTED`；再次增加时恢复为 `ACTIVE`。

## 正式业务接入

- 付费资料正文、预览和下载统一通过 `EntitlementService` 检查有效的 `CONTENT_ACCESS`。
- 考试发布和剩余额度查询统一通过 `EntitlementService` 消费或汇总 `EXAM_QUOTA`。
- 考试重复发布仍保持幂等，不会再次扣减额度。

## 前端页面与模拟支付标识

- `/commerce` 为统一权益商城，包含商品购买、我的订单和我的权益三个页签。
- 商品购买支持按资料、AI 次数包和考试发布次数包筛选；订单页支持状态筛选、取消待支付订单和发起模拟支付。
- 付费资料详情及发布者考试工作台均提供前往权益商城的入口。
- 管理后台的“订单管理”页支持跨用户订单筛选，并可查看订单项与模拟支付记录。
- 权益商城警示栏、支付按钮、确认框、支付结果、购买入口和管理员订单详情均明确说明“模拟支付”“不会真实扣款/不产生真实资金交易”，不连接任何真实支付渠道。

## OP-136 自动化验证

- 重复调用模拟支付接口返回同一成功支付记录，只发放一次权益。
- 即使客户端在创建订单请求中伪造单价和总额，也始终使用服务端商品价格生成订单快照并结算。
- 并发消费同一额度时只允许余额充足的请求成功，余额不会小于零。
- 支付流程中任一权益发放失败时，订单状态、支付记录和已发权益整体回滚。
- `CONTENT_ACCESS`、`AI_QUOTA`、`EXAM_QUOTA` 三类权益均按订单项规则准确发放。
