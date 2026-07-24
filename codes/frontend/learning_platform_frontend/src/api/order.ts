import { http } from '@/api/http'
import type { ApiResponse } from '@/types/api'
import type {
  AdminOrderListParams,
  Entitlement,
  EntitlementBalances,
  MockPaymentResult,
  Order,
  OrderListParams,
  OrderPage,
  Product,
  ProductType,
} from '@/types/order'

function data<T>(response: { data: ApiResponse<T> }): T {
  return response.data.data
}

export async function listProducts(productType?: ProductType): Promise<Product[]> {
  return data(await http.get<ApiResponse<Product[]>>('/products', { params: { productType } }))
}

export async function createOrder(productId: number, quantity: number): Promise<Order> {
  return data(
    await http.post<ApiResponse<Order>>('/orders', {
      items: [{ productId, quantity }],
    }),
  )
}

export async function listOrders(params: OrderListParams = {}): Promise<OrderPage> {
  return data(await http.get<ApiResponse<OrderPage>>('/orders', { params }))
}

export async function cancelOrder(orderId: number): Promise<Order> {
  return data(await http.post<ApiResponse<Order>>(`/orders/${orderId}/cancel`))
}

export async function mockPayOrder(orderId: number): Promise<MockPaymentResult> {
  return data(await http.post<ApiResponse<MockPaymentResult>>(`/orders/${orderId}/mock-pay`))
}

export async function listEntitlements(): Promise<Entitlement[]> {
  return data(await http.get<ApiResponse<Entitlement[]>>('/entitlements'))
}

export async function getEntitlementBalances(): Promise<EntitlementBalances> {
  return data(await http.get<ApiResponse<EntitlementBalances>>('/entitlements/balances'))
}

export async function listAdminOrders(
  params: AdminOrderListParams = {},
): Promise<OrderPage> {
  return data(await http.get<ApiResponse<OrderPage>>('/admin/orders', { params }))
}

export async function getAdminOrder(orderId: number): Promise<Order> {
  return data(await http.get<ApiResponse<Order>>(`/admin/orders/${orderId}`))
}
