export const LAND_CATEGORIES = [
  'Agricultural land',
  'Residential plots',
  'Commercial land',
  'Industrial land',
  'Mixed-use land',
  'Other land',
] as const

export type LandCategory = typeof LAND_CATEGORIES[number]

export interface LandListing {
  id: string
  title: string
  description: string
  district: string
  mandal: string
  village: string
  surveyNumber?: string
  extent: string
  category: LandCategory
  priceLakhs: number
  priceLabel: string
  unitPrice: string
  image: string
  imageIsRepresentative: boolean
  updated: string
  evidenceStatus: 'Broker-submitted · not independently verified'
  latitude?: number
  longitude?: number
  sourceReference?: string | null
}

export interface LandFilters {
  district?: string
  mandal?: string
  village?: string
  category?: string
  minBudget?: number
  maxBudget?: number
  minExtent?: number
  maxExtent?: number
  measurementUnit?: 'ACRE' | 'SQUARE_YARD' | 'SQUARE_FOOT' | 'HECTARE' | 'GUNTA'
}
