export type UserRole = 'BUYER' | 'BROKER' | 'ADMIN'

export interface AuthUser {
  id: string
  email: string
  fullName: string
  role: UserRole
  brokerApprovalStatus?: string | null
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: AuthUser
}

export interface RegisterRequest {
  email: string
  phone?: string
  fullName: string
  password: string
  accountType: 'BUYER' | 'BROKER'
  businessName?: string
}

export interface LoginRequest {
  email: string
  password: string
}
