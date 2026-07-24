import type { PageResult } from '@/types/api'

export type ProductType = 'CONTENT' | 'AI_PACKAGE' | 'EXAM_PACKAGE'
export type ProductStatus = 'ACTIVE' | 'INACTIVE'
export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'CLOSED' | 'REFUNDED'
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CLOSED' | 'REFUNDED'
export type EntitlementType = 'CONTENT_ACCESS' | 'AI_QUOTA' | 'EXAM_QUOTA'
export type EntitlementStatus = 'ACTIVE' | 'EXHAUSTED' | 'EXPIRED' | 'REVOKED'

export interface Product {
  id: number
  productCode: string
  productType: ProductType
  name: string
  description?: string
  resourceId?: number
  quantity?: number
  price: number
  status: ProductStatus
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface OrderItem {
  id: number
  productId?: number
  productCode: string
  productType: ProductType
  productName: string
  resourceId?: number
  unitPrice: number
  quantity: number
  entitlementQuantity?: number
  subtotalAmount: number
}

export interface PaymentRecord {
  id: number
  paymentNo: string
  provider: 'MOCK'
  providerTransactionNo: string
  amount: number
  status: PaymentStatus
  failureReason?: string
  paidAt?: string
  createdAt: string
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  status: OrderStatus
  totalAmount: number
  payableAmount: number
  paidAmount?: number
  paymentMethod?: string
  remark?: string
  expiresAt?: string
  paidAt?: string
  cancelledAt?: string
  items: OrderItem[]
  payments: PaymentRecord[]
  paymentNotice: string
  createdAt: string
  updatedAt: string
}

export interface MockPaymentResult {
  order: Order
  payment: PaymentRecord
  notice: string
}

export interface Entitlement {
  id: number
  userId: number
  entitlementType: EntitlementType
  resourceId?: number
  sourceOrderItemId?: number
  totalQuantity?: number
  availableQuantity?: number
  status: EntitlementStatus
  effectiveAt: string
  expiresAt?: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface EntitlementBalances {
  aiQuota: number
  examQuota: number
}

export interface OrderListParams {
  pageNumber?: number
  pageSize?: number
  status?: OrderStatus
}

export interface AdminOrderListParams extends OrderListParams {
  orderNo?: string
  userId?: number
}

export type OrderPage = PageResult<Order>
