import { useEffect, useState } from 'react'
import { ArrowRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { fetchLandListings } from '../services/landService'
import type { LandListing } from '../types/land'
import LandCard from './LandCard'

function LandSearch() {
  const [listings, setListings] = useState<LandListing[]>([])
  const [totalListings, setTotalListings] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const load = async () => {
      try {
        const data = await fetchLandListings({}, 0, 3)
        if (active) {
          setListings(data.content ?? [])
          setTotalListings(data.totalElements ?? 0)
        }
      } catch {
        if (active) {
          setError('Published listings could not be loaded. Please try again later.')
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [])

  return (
    <section className="section section-tinted">
      <div className="page-width">
        <div className="section-heading listings-heading">
          <div>
            <p className="eyebrow">Published inventory</p>
            <h2>Places worth a <em>closer look.</em></h2>
          </div>
          <Link to="/explore" className="button button-outline">
            {loading || error ? 'Browse listings' : `Explore all ${totalListings} published listing${totalListings === 1 ? '' : 's'}`} <ArrowRight size={16} />
          </Link>
        </div>
        {error ? (
          <div className="account-error" role="alert">
            {error}
          </div>
        ) : loading ? (
          <p className="empty-results" role="status">Loading published listings…</p>
        ) : listings.length === 0 ? (
          <p className="empty-results">There are no published listings to show yet.</p>
        ) : (
          <div className="demo-notice">
            <span className="demo-notice-dot" />
            <span><strong>{totalListings} published listing{totalListings === 1 ? '' : 's'}.</strong> Details are broker-submitted and not independently verified.</span>
          </div>
        )}
        <div className="land-grid">
          {listings.length > 0 ? listings.map((listing) => <LandCard key={listing.id} listing={listing} />) : null}
        </div>
      </div>
    </section>
  )
}

export default LandSearch
