import axios from 'axios'
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api'

const TOKEN_KEY = 'bhoomi-auth-token'
const USER_KEY = 'bhoomi-auth-user'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function setSession(authResponse: AuthResponse) {
  sessionStorage.setItem(TOKEN_KEY, authResponse.accessToken)
  sessionStorage.setItem(USER_KEY, JSON.stringify(authResponse.user))
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
}

export function getStoredUser() {
  const raw = sessionStorage.getItem(USER_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthResponse['user']
  } catch {
    clearSession()
    return null
  }
}

export function getStoredToken() {
  return sessionStorage.getItem(TOKEN_KEY)
}

export const authApi = {
  register: (payload: RegisterRequest) => api.post<AuthResponse>('/auth/register', payload),
  login: (payload: LoginRequest) => api.post<AuthResponse>('/auth/login', payload),
  forgotPassword: (email: string) => api.post('/auth/forgot-password', { email }),
  resetPassword: (token: string, newPassword: string) =>
    api.post('/auth/reset-password', { token, newPassword }),
}

export const listingApi = {
  list: (params: Record<string, string | number | undefined>) =>
    api.get('/listings', { params }),
  getById: (id: string) => api.get(`/listings/${id}`),
}

export const favoriteApi = {
  list: () => api.get('/buyer/favorites'),
  toggle: (listingId: string) => api.post(`/buyer/favorites/${listingId}`),
  remove: (listingId: string) => api.delete(`/buyer/favorites/${listingId}`),
}

export const buyerProfileApi = {
  get: () => api.get('/buyer/profile'),
  update: (payload: { fullName: string; phone?: string }) => api.put('/buyer/profile', payload),
}

export const brokerProfileApi = {
  get: () => api.get('/broker/profile'),
  update: (payload: { businessName: string; licenseNumber?: string }) => api.put('/broker/profile', payload),
}

export const brokerListingApi = {
  list: () => api.get('/broker/listings'),
  createDraft: (payload: Record<string, unknown>) => api.post('/broker/listings/drafts', payload),
  update: (listingId: string, payload: { title: string; description: string; askingPrice: number }) =>
    api.put(`/broker/listings/${listingId}`, payload),
  submit: (listingId: string) => api.post(`/broker/listings/${listingId}/submit`),
  updateStatus: (listingId: string, status: 'SOLD' | 'UNAVAILABLE' | 'ARCHIVED') =>
    api.patch(`/broker/listings/${listingId}/status`, { status }),
  confirmAvailability: (listingId: string) => api.post(`/broker/listings/${listingId}/availability/confirm`),
  uploadMedia: (listingId: string, file: File, caption?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (caption) formData.append('caption', caption)
    return api.post(`/broker/listings/${listingId}/media`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}

export const enquiryApi = {
  create: (listingId: string, message: string, contactPhone?: string) =>
    api.post(`/listings/${listingId}/enquiries`, { message, contactPhone }),
  listBuyer: () => api.get('/buyer/enquiries'),
  listBroker: () => api.get('/broker/enquiries'),
}

export const siteVisitApi = {
  create: (listingId: string, requestedAt: string, notes?: string) => api.post(`/listings/${listingId}/site-visits`, {
    requestedAt,
    notes: notes?.trim() ? notes.trim() : 'Requested site visit',
  }),
  listBuyer: () => api.get('/buyer/site-visits'),
  listBroker: () => api.get('/broker/site-visits'),
  updateStatus: (siteVisitId: string, status: 'REQUESTED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED', requestedAt?: string, notes?: string) =>
    api.patch(`/broker/site-visits/${siteVisitId}/status`, {
      status,
      requestedAt,
      notes: notes?.trim() ? notes.trim() : 'Status updated',
    }),
}

export const dashboardApi = {
  brokerListings: () => api.get('/broker/listings'),
  adminReviews: () => api.get('/admin/reviews'),
}

export const adminApi = {
  brokerQueue: (status = 'PENDING') => api.get('/admin/brokers', { params: { status } }),
  updateBrokerStatus: (id: string, status: 'APPROVED' | 'REJECTED', reviewNote?: string) =>
    api.patch(`/admin/brokers/${id}/status`, { status, reviewNote }),
  updateListingStatus: (id: string, status: 'UNDER_REVIEW' | 'PUBLISHED' | 'REJECTED', reviewNote?: string) =>
    api.patch(`/admin/listings/${id}/status`, { status, reviewNote }),
  documents: (status = 'PENDING') => api.get('/admin/documents', { params: { status } }),
  reviewDocument: (id: string, status: 'REVIEWED' | 'REJECTED', reviewNote?: string) =>
    api.patch(`/admin/documents/${id}/review`, { status, reviewNote }),
  downloadDocument: (id: string) => api.get(`/admin/documents/${id}/download`, { responseType: 'blob' }),
}

export const documentApi = {
  listForListing: (listingId: string) => api.get(`/broker/listings/${listingId}/documents`),
  upload: (listingId: string, file: File, description?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (description) formData.append('description', description)
    return api.post(`/broker/listings/${listingId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  download: (id: string) => api.get(`/broker/documents/${id}/download`, { responseType: 'blob' }),
}
