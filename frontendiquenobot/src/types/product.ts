import type { ProductStatus } from './enums'

export interface CategoryDto {
  id: string
  name: string
  description: string
  iconUrl: string
  displayOrder: number
  active: boolean
  productCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateCategoryRequest {
  name: string
  description?: string
  iconUrl?: string
  displayOrder?: number
  active?: boolean
}

export interface ProductDto {
  id: string
  sku: string
  name: string
  description: string
  shortDescription: string
  price: number
  compareAtPrice: number
  costPrice: number
  category: CategoryDto | null
  stockQuantity: number
  lowStockThreshold: number
  status: ProductStatus
  imageUrl: string
  images: string
  weight: number | null
  width: number | null
  height: number | null
  length: number | null
  featured: boolean
  tags: string
  available: boolean
  lowStock: boolean
  hasDiscount: boolean
  discountAmount: number
  discountPercentage: number
  createdAt: string
  updatedAt: string
}

export interface CreateProductRequest {
  sku?: string
  name: string
  description?: string
  shortDescription?: string
  price: number
  compareAtPrice?: number
  costPrice?: number
  categoryId?: string
  stockQuantity: number
  lowStockThreshold?: number
  status: ProductStatus
  imageUrl?: string
  images?: string
  weight?: number
  width?: number
  height?: number
  length?: number
  featured?: boolean
  tags?: string
}
