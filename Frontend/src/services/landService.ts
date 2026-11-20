import type { LandFilters, LandListing } from '../types/land'
import { API_BASE_URL, listingApi } from './api'

export interface ListingApiResponse {
  id: string
  title: string
  description: string
  askingPrice: number | string
  currency?: string
  status?: string
  createdAt?: string
  publishedAt?: string
  updatedAt?: string
  sourceReference?: string | null
  parcel?: {
    id?: string
    district?: string
    mandal?: string
    village?: string
    surveyNumber?: string
    subdivisionNumber?: string
    extent?: number | string
    measurementUnit?: string
    landType?: string
    latitude?: number
    longitude?: number
  }
  media?: Array<{ id?: string; url?: string; mediaType?: string }>
}

export interface ListingPageResponse {
  content: ListingApiResponse[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export function mapApiListingToLandListing(apiListing: ListingApiResponse): LandListing {
  const parcel = apiListing.parcel ?? {}
  const landTypeLabel = parcel.landType ? mapLandTypeToCategory(parcel.landType) : 'Other land'
  const extent = parcel.extent ?? 0
  const extentLabel = typeof extent === 'number' ? `${extent}` : String(extent)
  const priceNumber = Number(apiListing.askingPrice ?? 0)
  const priceLabel = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: apiListing.currency ?? 'INR',
    maximumFractionDigits: 0,
  }).format(priceNumber)
  const priceLakhs = Math.round((priceNumber / 100000) * 100) / 100

  return {
    id: apiListing.id,
    title: apiListing.title,
    description: apiListing.description ?? '',
    district: parcel.district ?? 'Not provided',
    mandal: parcel.mandal ?? 'Not provided',
    village: parcel.village ?? 'Not provided',
    surveyNumber: parcel.surveyNumber ?? parcel.subdivisionNumber ?? 'Not supplied',
    extent: `${extentLabel} ${parcel.measurementUnit ? parcel.measurementUnit.toLowerCase().replace('_', ' ') : 'units'}`,
    category: landTypeLabel,
    priceLakhs,
    priceLabel,
    unitPrice: `Total asking price · ${apiListing.currency ?? 'INR'}`,
    image: getListingImage(apiListing),
    imageIsRepresentative: !apiListing.media?.some((item) => item.url),
    updated: apiListing.updatedAt ? new Date(apiListing.updatedAt).toLocaleDateString('en-IN') : 'Recently listed',
    evidenceStatus: 'Broker-submitted · not independently verified',
    latitude: parcel.latitude,
    longitude: parcel.longitude,
    sourceReference: apiListing.sourceReference,
  }
}

export async function fetchLandListings(
  filters: LandFilters = {},
  page = 0,
  size = 20,
  sortBy: 'createdAt' | 'askingPrice' | 'title' = 'createdAt',
  direction: 'ASC' | 'DESC' = 'DESC',
) {
  const params: Record<string, string | number | undefined> = {
    page,
    size,
    sortBy,
    direction,
    district: filters.district,
    mandal: filters.mandal,
    village: filters.village,
    minPrice: filters.minBudget !== undefined ? filters.minBudget * 100000 : undefined,
    maxPrice: filters.maxBudget !== undefined ? filters.maxBudget * 100000 : undefined,
    landType: mapCategoryToLandType(filters.category),
    minExtent: filters.minExtent,
    maxExtent: filters.maxExtent,
    measurementUnit: filters.measurementUnit,
  }

  const response = await listingApi.list(params)
  const payload = response.data as ListingPageResponse
  return {
    ...payload,
    content: (payload.content ?? []).map(mapApiListingToLandListing),
  }
}

export async function fetchLandListingById(id: string) {
  const response = await listingApi.getById(id)
  return mapApiListingToLandListing(response.data as ListingApiResponse)
}

export function getListingImage(apiListing: ListingApiResponse) {
  const mediaUrl = apiListing.media?.find((item) => item.url)?.url
  if (mediaUrl) {
    if (/^https?:\/\//i.test(mediaUrl)) return mediaUrl
    const apiRoot = new URL(API_BASE_URL, window.location.origin)
    const apiPath = apiRoot.pathname.replace(/\/api\/?$/, '')
    return new URL(`${apiPath}${mediaUrl}`, apiRoot.origin).toString()
  }

  return 'https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=1200&q=85'
}

function mapLandTypeToCategory(value: string): LandListing['category'] {
  const normalized = value.trim().toUpperCase()
  const mapping: Record<string, LandListing['category']> = {
    AGRICULTURAL: 'Agricultural land',
    RESIDENTIAL: 'Residential plots',
    COMMERCIAL: 'Commercial land',
    INDUSTRIAL: 'Industrial land',
    MIXED_USE: 'Mixed-use land',
    OTHER: 'Other land',
  }
  return mapping[normalized] ?? 'Other land'
}

function mapCategoryToLandType(category?: string) {
  if (!category) return undefined

  const normalized = category.trim().toLowerCase()
  const mapping: Record<string, string> = {
    'agricultural land': 'AGRICULTURAL',
    'residential plots': 'RESIDENTIAL',
    'commercial land': 'COMMERCIAL',
    'industrial land': 'INDUSTRIAL',
    'mixed-use land': 'MIXED_USE',
    'other land': 'OTHER',
  }

  return mapping[normalized]
}
