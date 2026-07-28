<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import {
  cancelOrder,
  createOrder,
  getEntitlementBalances,
  listEntitlements,
  listOrders,
  listProducts,
  mockPayOrder,
} from '@/api/order'
import SectionPageHeader from '@/components/SectionPageHeader.vue'
import type {
  Entitlement,
  EntitlementBalances,
  EntitlementStatus,
  EntitlementType,
  Order,
  OrderStatus,
  Product,
  ProductType,
} from '@/types/order'

const route = useRoute()
const activeTab = ref(typeof route.query.tab === 'string' ? route.query.tab : 'products')
const productsLoading = ref(false)
const ordersLoading = ref(false)
const entitlementsLoading = ref(false)
const products = ref<Product[]>([])
const orders = ref<Order[]>([])
const entitlements = ref<Entitlement[]>([])
const balances = ref<EntitlementBalances>({
  aiQuota: 0,
  examQuota: 0,
  examOverallAiQuota: 0,
  examPersonalAiQuota: 0,
})
const orderTotal = ref(0)
const creatingId = ref<number>()
const payingId = ref<number>()
const cancellingId = ref<number>()
const quantities = reactive<Record<number, number>>({})
const productType = ref<ProductType | 'ALL'>(
  route.query.type === 'CONTENT' ||
    route.query.type === 'AI_PACKAGE' ||
    route.query.type === 'EXAM_PACKAGE' ||
    route.query.type === 'EXAM_OVERALL_AI_PACKAGE' ||
    route.query.type === 'EXAM_PERSONAL_AI_PACKAGE'
    ? route.query.type
    : 'ALL',
)
const orderFilters = reactive({
  status: undefined as OrderStatus | undefined,
  pageNumber: 1,
  pageSize: 10,
})

const visibleProducts = computed(() =>
  productType.value === 'ALL'
    ? products.value
    : products.value.filter((product) => product.productType === productType.value),
)
const ownedContentResourceIds = computed(
  () =>
    new Set(
      entitlements.value
        .filter(
          (item) =>
            item.entitlementType === 'CONTENT_ACCESS' &&
            item.status === 'ACTIVE' &&
            item.resourceId != null,
        )
        .map((item) => item.resourceId as number),
    ),
)

const productTypeLabels: Record<ProductType, string> = {
  CONTENT: '付费资料',
  AI_PACKAGE: 'AI 次数包',
  EXAM_PACKAGE: '考试发布次数包',
  EXAM_OVERALL_AI_PACKAGE: '考试整体 AI 分析次数包',
  EXAM_PERSONAL_AI_PACKAGE: '考试个人 AI 分析次数包',
}
const orderStatusLabels: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '支付成功',
  CANCELLED: '已取消',
  CLOSED: '已关闭',
  REFUNDED: '已退款',
}
const entitlementTypeLabels: Record<EntitlementType, string> = {
  CONTENT_ACCESS: '资料访问权',
  AI_QUOTA: 'AI 使用次数',
  EXAM_QUOTA: '考试发布次数',
  EXAM_OVERALL_AI_QUOTA: '考试整体 AI 分析次数',
  EXAM_PERSONAL_AI_QUOTA: '考试个人 AI 分析次数',
}
const entitlementStatusLabels: Record<EntitlementStatus, string> = {
  ACTIVE: '可用',
  EXHAUSTED: '已用尽',
  EXPIRED: '已过期',
  REVOKED: '已撤销',
}

function money(value: number): string {
  return `¥${Number(value).toFixed(2)}`
}

function dateTime(value?: string): string {
  return value ? new Date(value).toLocaleString() : '—'
}

function orderTagType(status: OrderStatus): 'success' | 'warning' | 'info' | 'danger' {
  if (status === 'PAID') return 'success'
  if (status === 'PENDING_PAYMENT') return 'warning'
  if (status === 'REFUNDED') return 'danger'
  return 'info'
}

function purchaseQuantity(product: Product): number {
  return product.productType === 'CONTENT' ? 1 : (quantities[product.id] ?? 1)
}

function ownsContent(product: Product): boolean {
  return (
    product.productType === 'CONTENT' &&
    product.resourceId != null &&
    ownedContentResourceIds.value.has(product.resourceId)
  )
}

async function loadProducts(): Promise<void> {
  productsLoading.value = true
  try {
    products.value = await listProducts()
    for (const product of products.value) quantities[product.id] ??= 1
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败')
  } finally {
    productsLoading.value = false
  }
}

async function loadOrders(): Promise<void> {
  ordersLoading.value = true
  try {
    const page = await listOrders(orderFilters)
    orders.value = page.items
    orderTotal.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    ordersLoading.value = false
  }
}

async function loadEntitlements(): Promise<void> {
  entitlementsLoading.value = true
  try {
    ;[entitlements.value, balances.value] = await Promise.all([
      listEntitlements(),
      getEntitlementBalances(),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '权益加载失败')
  } finally {
    entitlementsLoading.value = false
  }
}

async function buy(product: Product): Promise<void> {
  if (ownsContent(product)) {
    ElMessage.warning('你已拥有该付费资料，无需重复购买')
    return
  }
  const quantity = purchaseQuantity(product)
  const total = Number(product.price) * quantity
  try {
    await ElMessageBox.confirm(
      `将创建金额为 ${money(total)} 的订单。`,
      '创建支付订单',
      {
        confirmButtonText: '确认创建',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    creatingId.value = product.id
    await createOrder(product.id, quantity)
    ElMessage.success('订单已创建，请在“我的订单”中进行支付')
    activeTab.value = 'orders'
    orderFilters.pageNumber = 1
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '创建订单失败')
  } finally {
    creatingId.value = undefined
  }
}

async function pay(order: Order): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `本次支付 ${money(order.payableAmount)}`,
      '支付确认',
      {
        confirmButtonText: '确认支付',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    payingId.value = order.id
    const result = await mockPayOrder(order.id)
    ElMessage.success(result.notice)
    await Promise.all([loadOrders(), loadEntitlements()])
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '支付失败')
  } finally {
    payingId.value = undefined
  }
}

async function cancel(order: Order): Promise<void> {
  try {
    await ElMessageBox.confirm('确认取消这张未支付订单吗？', '取消订单', {
      type: 'warning',
    })
    cancellingId.value = order.id
    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    await loadOrders()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error instanceof Error ? error.message : '取消失败')
  } finally {
    cancellingId.value = undefined
  }
}

function searchOrders(): void {
  orderFilters.pageNumber = 1
  void loadOrders()
}

onMounted(() => Promise.all([loadProducts(), loadOrders(), loadEntitlements()]))
</script>

<template>
  <section class="commerce-page">
    <div class="page-container">
      <SectionPageHeader
        eyebrow="SIMULATED COMMERCE"
        title="商品、订单与我的权益"
        description="购买学习资料、AI 次数和考试发布次数，并集中查看测试订单与权益余额"
      />

      <!-- <el-alert
        class="mock-alert"
        title="当前为模拟支付环境"
        description="所有价格、订单和支付按钮仅用于功能测试，不连接真实支付渠道，不会产生真实资金交易。"
        type="warning"
        show-icon
        :closable="false"
      /> -->

      <div class="commerce-card">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品购买" name="products">
            <div class="product-toolbar">
              <el-radio-group v-model="productType">
                <el-radio-button value="ALL">全部商品</el-radio-button>
                <el-radio-button value="CONTENT">付费资料</el-radio-button>
                <el-radio-button value="AI_PACKAGE">AI 次数包</el-radio-button>
                <el-radio-button value="EXAM_PACKAGE">考试次数包</el-radio-button>
                <el-radio-button value="EXAM_OVERALL_AI_PACKAGE">
                  考试整体分析
                </el-radio-button>
                <el-radio-button value="EXAM_PERSONAL_AI_PACKAGE">
                  考试个人分析
                </el-radio-button>
              </el-radio-group>
            </div>
            <div v-loading="productsLoading" class="product-grid">
              <article v-for="product in visibleProducts" :key="product.id" class="product-card">
                <el-tag effect="plain">{{ productTypeLabels[product.productType] }}</el-tag>
                <h2>{{ product.name }}</h2>
                <p>{{ product.description || '学习平台测试商品' }}</p>
                <div class="product-benefit">
                  <template v-if="product.productType === 'CONTENT'">
                    资料 ID {{ product.resourceId }} · 永久访问权
                  </template>
                  <template v-else>
                    每件包含 {{ product.quantity }} 次
                  </template>
                </div>
                <div class="product-footer">
                  <strong>{{ money(product.price) }}</strong>
                  <el-input-number
                    v-if="product.productType !== 'CONTENT'"
                    v-model="quantities[product.id]"
                    :min="1"
                    :max="99"
                    size="small"
                  />
                </div>
                <el-button
                  type="primary"
                  :disabled="ownsContent(product)"
                  :loading="creatingId === product.id"
                  @click="buy(product)"
                >
                  {{ ownsContent(product) ? '已拥有该资料' : '创建订单' }}
                </el-button>
                <small v-if="ownsContent(product)" class="owned-note">
                  该资料已在“我的权益”中，无需再次购买
                </small>
                
              </article>
              <el-empty v-if="!productsLoading && visibleProducts.length === 0" description="暂无在售商品" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="我的订单" name="orders">
            <!-- <el-alert
              title="订单中的“支付”均指模拟支付"
              type="warning"
              show-icon
              :closable="false"
            /> -->
            <div class="order-toolbar">
              <el-select v-model="orderFilters.status" clearable placeholder="全部订单状态">
                <el-option label="待支付" value="PENDING_PAYMENT" />
                <el-option label="支付成功" value="PAID" />
                <el-option label="已取消" value="CANCELLED" />
                <el-option label="已关闭" value="CLOSED" />
                <el-option label="已退款" value="REFUNDED" />
              </el-select>
              <el-button type="primary" @click="searchOrders">查询</el-button>
            </div>

            <div v-loading="ordersLoading" class="order-list">
              <article v-for="order in orders" :key="order.id" class="order-card">
                <header>
                  <div>
                    <strong>{{ order.orderNo }}</strong>
                    <span>创建于 {{ dateTime(order.createdAt) }}</span>
                  </div>
                  <el-tag :type="orderTagType(order.status)">
                    {{ orderStatusLabels[order.status] }}
                  </el-tag>
                </header>
                <div class="order-items">
                  <div v-for="item in order.items" :key="item.id">
                    <span>{{ item.productName }} × {{ item.quantity }}</span>
                    <strong>{{ money(item.subtotalAmount) }}</strong>
                  </div>
                </div>
                <footer>
                  <span>应付：<strong>{{ money(order.payableAmount) }}</strong></span>
                  <div v-if="order.status === 'PENDING_PAYMENT'">
                    <el-button
                      :loading="cancellingId === order.id"
                      @click="cancel(order)"
                    >
                      取消订单
                    </el-button>
                    <el-button
                      type="warning"
                      :loading="payingId === order.id"
                      @click="pay(order)"
                    >
                      支付
                    </el-button>
                  </div>
                  <!-- <span v-else-if="order.status === 'PAID'" class="paid-note">
                    已完成模拟支付 · 不代表真实交易
                  </span> -->
                </footer>
              </article>
              <el-empty v-if="!ordersLoading && orders.length === 0" description="暂无订单" />
            </div>
            <el-pagination
              v-model:current-page="orderFilters.pageNumber"
              v-model:page-size="orderFilters.pageSize"
              background
              layout="total, prev, pager, next"
              :total="orderTotal"
              @current-change="loadOrders"
            />
          </el-tab-pane>

          <el-tab-pane label="我的权益" name="entitlements">
            <div v-loading="entitlementsLoading">
              <div class="balance-grid">
                <article><span>AI 可用次数</span><strong>{{ balances.aiQuota }}</strong></article>
                <article><span>考试发布可用次数</span><strong>{{ balances.examQuota }}</strong></article>
                <article>
                  <span>考试整体 AI 分析次数</span>
                  <strong>{{ balances.examOverallAiQuota }}</strong>
                </article>
                <article>
                  <span>考试个人 AI 分析次数</span>
                  <strong>{{ balances.examPersonalAiQuota }}</strong>
                </article>
                <article>
                  <span>有效资料访问权</span>
                  <strong>
                    {{ entitlements.filter((item) => item.entitlementType === 'CONTENT_ACCESS' && item.status === 'ACTIVE').length }}
                  </strong>
                </article>
              </div>
              <el-table :data="entitlements">
                <el-table-column label="权益类型" min-width="170">
                  <template #default="{ row }">
                    {{ entitlementTypeLabels[row.entitlementType as EntitlementType] }}
                  </template>
                </el-table-column>
                <el-table-column label="权益内容" min-width="180">
                  <template #default="{ row }">
                    <span v-if="row.entitlementType === 'CONTENT_ACCESS'">
                      资料 ID {{ row.resourceId }}
                    </span>
                    <span v-else>{{ row.availableQuantity }} / {{ row.totalQuantity }} 次</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                      {{ entitlementStatusLabels[row.status as EntitlementStatus] }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="生效时间" min-width="180">
                  <template #default="{ row }">{{ dateTime(row.effectiveAt) }}</template>
                </el-table-column>
                <template #empty><el-empty description="暂无权益，可先购买测试商品" /></template>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </section>
</template>

<style scoped>
.commerce-page { min-height: calc(100vh - 145px); padding: 48px 0 76px; background: linear-gradient(180deg, #f3f7ff, #f7f9fc 360px); }
.mock-alert { margin-bottom: 20px; }
.commerce-card { border: 1px solid var(--lp-border); border-radius: 20px; background: #fff; box-shadow: var(--lp-shadow); padding: 10px 24px 26px; }
.product-toolbar, .order-toolbar { display: flex; align-items: center; gap: 12px; margin: 8px 0 22px; }
.order-toolbar .el-select { width: 220px; }
.product-grid { display: grid; min-height: 220px; gap: 18px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.product-card { display: flex; min-width: 0; border: 1px solid var(--lp-border); border-radius: 16px; flex-direction: column; background: #fbfcff; padding: 22px; }
.product-card h2 { margin: 16px 0 8px; font-size: 20px; }
.product-card p { min-height: 48px; margin: 0; color: var(--lp-text-secondary); font-size: 13px; line-height: 1.7; }
.product-benefit { border-radius: 10px; margin-top: 16px; color: #344054; background: #eef4ff; font-size: 13px; padding: 11px; }
.product-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin: 22px 0 14px; }
.product-footer strong { color: var(--lp-primary); font-size: 25px; }
.product-card small { margin-top: 9px; color: #b54708; text-align: center; }
.product-card .owned-note { color: #039855; }
.order-list { display: grid; min-height: 200px; gap: 14px; }
.order-card { border: 1px solid var(--lp-border); border-radius: 15px; overflow: hidden; }
.order-card header, .order-card footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; background: #f8faff; padding: 15px 18px; }
.order-card header div { display: flex; flex-direction: column; gap: 5px; }
.order-card header span { color: var(--lp-text-secondary); font-size: 12px; }
.order-items { padding: 8px 18px; }
.order-items div { display: flex; border-bottom: 1px solid #f0f2f5; justify-content: space-between; padding: 11px 0; }
.order-items div:last-child { border-bottom: 0; }
.order-card footer > span strong { color: var(--lp-primary); font-size: 18px; }
.paid-note { color: var(--lp-success); font-size: 13px; font-weight: 700; }
.el-pagination { justify-content: flex-end; margin-top: 22px; }
.balance-grid { display: grid; gap: 16px; margin: 10px 0 24px; grid-template-columns: repeat(3, 1fr); }
.balance-grid article { display: flex; border: 1px solid #dbe6ff; border-radius: 14px; flex-direction: column; gap: 9px; background: #f5f8ff; padding: 20px; }
.balance-grid span { color: var(--lp-text-secondary); font-size: 13px; }
.balance-grid strong { color: var(--lp-primary); font-size: 30px; }
@media (max-width: 900px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) {
  .commerce-card { padding: 8px 12px 18px; }
  .product-grid, .balance-grid { grid-template-columns: 1fr; }
  .product-toolbar { align-items: stretch; overflow-x: auto; }
  .order-card header, .order-card footer { align-items: stretch; flex-direction: column; }
}
</style>
