import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowDown, ArrowRight, MapPin, Search } from 'lucide-react'
import { LAND_CATEGORIES, type LandFilters } from '../types/land'

function Hero() {
  const navigate = useNavigate()
  const [filters, setFilters] = useState<LandFilters>({})
  const update = (key: keyof LandFilters, value: string) => {
    setFilters((current) => {
      const next = { ...current, [key]: value || undefined }
      return next
    })
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const query = new URLSearchParams()
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== '') query.set(key, String(value))
    })
    const queryString = query.toString()
    navigate(`/explore${queryString ? `?${queryString}` : ''}`)
  }

  return (
    <section className="hero">
      <div className="hero-image" role="img" aria-label="Green agricultural fields in Telangana" />
      <div className="hero-shade" />
      <div className="hero-content page-width">
        <p className="eyebrow eyebrow-light"><span className="eyebrow-dot" /> A clearer view of Telangana</p>
        <h1>Discover land.<br /><em>Invest with confidence.</em></h1>
        <p className="hero-description">
          Explore agricultural land, residential plots and commercial properties with better location insights.
        </p>
        <a className="hero-discover" href="#categories">
          Discover what matters <ArrowDown size={16} />
        </a>
        <form className="search-panel" onSubmit={submit}>
          <div className="search-fields">
            <label className="search-field">
              <span><MapPin size={14} /> District</span>
              <input value={filters.district ?? ''} placeholder="All districts" onChange={(event) => update('district', event.target.value)} />
            </label>
            <label className="search-field">
              <span>Mandal</span>
              <input value={filters.mandal ?? ''} placeholder="All mandals" onChange={(event) => update('mandal', event.target.value)} />
            </label>
            <label className="search-field">
              <span>Village</span>
              <input value={filters.village ?? ''} placeholder="All villages" onChange={(event) => update('village', event.target.value)} />
            </label>
            <label className="search-field">
              <span>Land category</span>
              <select value={filters.category ?? ''} onChange={(event) => update('category', event.target.value)}>
                <option value="">All categories</option>
                {LAND_CATEGORIES.map((category) => <option key={category}>{category}</option>)}
              </select>
            </label>
            <label className="search-field budget-field">
              <span>Min. budget</span>
              <input
                type="number"
                min="0"
                step="1"
                value={filters.minBudget ?? ''}
                placeholder="No minimum"
                onChange={(event) => setFilters((current) => ({
                  ...current,
                  minBudget: event.target.value ? Number(event.target.value) : undefined,
                }))}
              />
              <small>₹ lakh</small>
            </label>
            <label className="search-field budget-field">
              <span>Max. budget</span>
              <input
                type="number"
                min="0"
                step="1"
                value={filters.maxBudget ?? ''}
                placeholder="No maximum"
                onChange={(event) => setFilters((current) => ({
                  ...current,
                  maxBudget: event.target.value ? Number(event.target.value) : undefined,
                }))}
              />
              <small>₹ lakh</small>
            </label>
          </div>
          <button className="button button-gold search-button" type="submit">
            <Search size={17} /> Find land <ArrowRight size={16} />
          </button>
        </form>
        <p className="hero-note">Search published broker-submitted listings · details are not independent title verification</p>
      </div>
    </section>
  )
}

export default Hero
