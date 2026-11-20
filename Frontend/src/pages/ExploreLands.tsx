import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowUpRight, MapPin, SlidersHorizontal } from 'lucide-react'
import LandCard from '../components/LandCard'
import { fetchLandListings } from '../services/landService'
import { LAND_CATEGORIES, type LandFilters, type LandListing } from '../types/land'

function ExploreLands() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [results, setResults] = useState<LandListing[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const pageIndex = nonNegativeInteger(searchParams.get('page'))
  const sortBy = searchParams.get('sortBy') === 'askingPrice' ? 'askingPrice' : 'createdAt'
  const direction = searchParams.get('direction') === 'ASC' ? 'ASC' : 'DESC'

  const filters: LandFilters = {
    district: searchParams.get('district') || undefined,
    mandal: searchParams.get('mandal') || undefined,
    village: searchParams.get('village') || undefined,
    category: searchParams.get('category') || undefined,
    minBudget: searchParams.has('minBudget') ? Number(searchParams.get('minBudget')) : undefined,
    maxBudget: searchParams.has('maxBudget') ? Number(searchParams.get('maxBudget')) : undefined,
    minExtent: searchParams.has('minExtent') ? Number(searchParams.get('minExtent')) : undefined,
    maxExtent: searchParams.has('maxExtent') ? Number(searchParams.get('maxExtent')) : undefined,
    measurementUnit: (searchParams.get('measurementUnit') as LandFilters['measurementUnit']) || undefined,
  }

  useEffect(() => {
    let active = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')
        const page = await fetchLandListings(filters, pageIndex, 20, sortBy, direction)
        if (!active) return
        setResults(page.content ?? [])
        setTotalPages(page.totalPages ?? 0)
        setTotalElements(page.totalElements ?? 0)
      } catch (requestError) {
        if (!active) return
        setResults([])
        setError('Unable to load live listings from the Bhoomi backend. Please check that the backend is running and the API is available.')
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    const timer = window.setTimeout(() => void load(), 250)
    return () => {
      window.clearTimeout(timer)
      active = false
    }
  }, [filters.district, filters.mandal, filters.village, filters.category, filters.minBudget, filters.maxBudget,
    filters.minExtent, filters.maxExtent, filters.measurementUnit, pageIndex, sortBy, direction])

  const setFilter = (key: keyof LandFilters, value: string) => {
    const next = new URLSearchParams(searchParams)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page')
    if (key === 'district') {
      next.delete('mandal')
      next.delete('village')
    } else if (key === 'mandal') {
      next.delete('village')
    }
    setSearchParams(next, { replace: true })
  }

  const setSort = (value: string) => {
    const [nextSort, nextDirection] = value.split('-')
    const next = new URLSearchParams(searchParams)
    next.set('sortBy', nextSort)
    next.set('direction', nextDirection)
    next.delete('page')
    setSearchParams(next)
  }

  const setPage = (page: number) => {
    const next = new URLSearchParams(searchParams)
    if (page > 0) next.set('page', String(page))
    else next.delete('page')
    setSearchParams(next)
  }

  const resetFilters = () => setSearchParams({})

  return (
    <div className="explore-page">
      <section className="page-banner">
        <div className="page-width">
          <p className="eyebrow"><MapPin size={14} /> Telangana, India</p>
          <h1>Explore land with <em>more context.</em></h1>
          <p>Browse published listings from the live Bhoomi backend.</p>
          <div className="breadcrumb"><Link to="/">Home</Link><span>/</span><span>Explore lands</span></div>
        </div>
      </section>
      <section className="page-width explore-content">
        <div className="demo-notice">
          <span className="demo-notice-dot" />
          <span><strong>Live inventory.</strong> Listings shown here are fetched from the backend and only published listings are included.</span>
        </div>
        <div className="explore-layout">
          <aside className="filter-panel" aria-label="Filter land listings">
            <div className="filter-heading"><SlidersHorizontal size={17} /><h2>Refine your search</h2></div>
            <label className="filter-control">
              District
              <input value={filters.district ?? ''} maxLength={120} placeholder="Any district" onChange={(event) => setFilter('district', event.target.value)} />
            </label>
            <label className="filter-control">
              Mandal
              <input value={filters.mandal ?? ''} maxLength={120} placeholder="Any mandal" onChange={(event) => setFilter('mandal', event.target.value)} />
            </label>
            <label className="filter-control">
              Village
              <input value={filters.village ?? ''} maxLength={120} placeholder="Any village" onChange={(event) => setFilter('village', event.target.value)} />
            </label>
            <label className="filter-control">
              Category
              <select value={filters.category ?? ''} onChange={(event) => setFilter('category', event.target.value)}>
                <option value="">All categories</option>
                {LAND_CATEGORIES.map((category) => <option key={category}>{category}</option>)}
              </select>
            </label>
            <div className="filter-budget">
              <label className="filter-control">
                Minimum (₹ lakh)
                <input
                  type="number"
                  min="0"
                  inputMode="numeric"
                  value={filters.minBudget ?? ''}
                  placeholder="No minimum"
                  onChange={(event) => setFilter('minBudget', event.target.value)}
                />
              </label>
              <label className="filter-control">
                Maximum (₹ lakh)
                <input
                  type="number"
                  min="0"
                  inputMode="numeric"
                  value={filters.maxBudget ?? ''}
                  placeholder="No maximum"
                  onChange={(event) => setFilter('maxBudget', event.target.value)}
                />
              </label>
            </div>
            <label className="filter-control">
              Minimum extent
              <input type="number" min="0" step="0.01" value={filters.minExtent ?? ''} onChange={(event) => setFilter('minExtent', event.target.value)} />
            </label>
            <label className="filter-control">
              Maximum extent
              <input type="number" min="0" step="0.01" value={filters.maxExtent ?? ''} onChange={(event) => setFilter('maxExtent', event.target.value)} />
            </label>
            <label className="filter-control">
              Extent unit
              <select value={filters.measurementUnit ?? 'ACRE'} onChange={(event) => setFilter('measurementUnit', event.target.value)}>
                <option value="ACRE">Acres</option>
                <option value="GUNTA">Guntas</option>
                <option value="SQUARE_YARD">Square yards</option>
                <option value="SQUARE_FOOT">Square feet</option>
                <option value="HECTARE">Hectares</option>
              </select>
            </label>
            <button type="button" className="filter-reset" onClick={resetFilters}>Clear all filters</button>
          </aside>
          <div className="results-column">
            <div className="results-heading">
              <div><p className="eyebrow">Land discovery</p><h2>{totalElements} listing{totalElements === 1 ? '' : 's'} to explore</h2></div>
              <label className="filter-control">
                Sort listings
                <select value={`${sortBy}-${direction}`} onChange={(event) => setSort(event.target.value)}>
                  <option value="createdAt-DESC">Newest first</option>
                  <option value="createdAt-ASC">Oldest first</option>
                  <option value="askingPrice-ASC">Price: low to high</option>
                  <option value="askingPrice-DESC">Price: high to low</option>
                </select>
              </label>
            </div>

            {error && <div className="account-error" role="alert">{error}</div>}

            {loading ? (
              <div className="empty-results">
                <MapPin size={25} />
                <h3>Loading live listings…</h3>
                <p>Fetching the published property inventory from the backend.</p>
              </div>
            ) : results.length ? (
              <>
                <div className="land-grid explore-grid">
                  {results.map((listing) => <LandCard key={listing.id} listing={listing} />)}
                </div>
                {totalPages > 1 && <div className="pagination-controls" aria-label="Listing pages">
                  <button className="button button-outline" type="button" onClick={() => setPage(pageIndex - 1)} disabled={pageIndex === 0}>Previous</button>
                  <span>Page {pageIndex + 1} of {totalPages}</span>
                  <button className="button button-outline" type="button" onClick={() => setPage(pageIndex + 1)} disabled={pageIndex + 1 >= totalPages}>Next</button>
                </div>}
              </>
            ) : (
              <div className="empty-results">
                <MapPin size={25} />
                <h3>No matching listings</h3>
                <p>Try different filters or clear the search to see all published listings.</p>
                <button className="button button-outline" type="button" onClick={resetFilters}>Clear filters <ArrowUpRight size={15} /></button>
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

function nonNegativeInteger(value: string | null) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0
}

export default ExploreLands
