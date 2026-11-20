import { useEffect, useState } from 'react'
import { ArrowUpRight, FileSearch, Heart, MapPin } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { favoriteApi } from '../services/api'
import type { LandListing } from '../types/land'

function LandCard({ listing }: { listing: LandListing }) {
  const { isAuthenticated, isBuyer } = useAuth()
  const navigate = useNavigate()
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)
  const [imageUnavailable, setImageUnavailable] = useState(false)

  useEffect(() => {
    if (!isAuthenticated || !isBuyer) {
      setSaved(false)
      return
    }

    const loadFavoriteState = async () => {
      try {
        const response = await favoriteApi.list()
        const favorites = Array.isArray(response.data) ? response.data : []
        setSaved(favorites.some((item: { listingId?: string }) => item.listingId === listing.id))
      } catch {
        setSaved(false)
      }
    }

    void loadFavoriteState()
  }, [isAuthenticated, isBuyer, listing.id])

  const toggleFavorite = async () => {
    if (!isAuthenticated || !isBuyer) {
      navigate('/login', { state: { from: `/lands/${listing.id}` } })
      return
    }

    setLoading(true)
    try {
      const response = await favoriteApi.toggle(listing.id)
      setSaved(Boolean(response.data?.saved))
    } catch {
      setSaved(false)
    } finally {
      setLoading(false)
    }
  }

  return (
    <article className="land-card">
      <div className="land-card-image">
        <Link to={`/lands/${listing.id}`} className="image-link" aria-label={`View ${listing.title}`}>
          {!imageUnavailable && (
            <img
              src={listing.image}
              alt={`Landscape representing ${listing.category.toLowerCase()}`}
              loading="lazy"
              onError={() => setImageUnavailable(true)}
            />
          )}
        </Link>
        <span className="demo-label">Published listing</span>
        {listing.imageIsRepresentative && <span className="image-attribution">Representative image</span>}
        <button
          className={`favorite-button${saved ? ' is-saved' : ''}`}
          type="button"
          aria-label={saved ? 'Remove from saved lands' : 'Save this land'}
          aria-pressed={saved}
          onClick={toggleFavorite}
          disabled={loading}
        >
          <Heart size={17} fill={saved ? 'currentColor' : 'none'} />
        </button>
      </div>
      <div className="land-card-body">
        <div className="land-card-topline">
          <span className="category-tag">{listing.category}</span>
          <span className="freshness">{listing.updated}</span>
        </div>
        <Link className="land-title-link" to={`/lands/${listing.id}`}>
          <h3>{listing.title}</h3>
        </Link>
        <p className="land-location"><MapPin size={14} /> {listing.village}, {listing.mandal}, {listing.district}</p>
        <div className="land-facts">
          <span><small>Extent</small>{listing.extent}</span>
          <span><small>Survey number</small>{listing.surveyNumber ?? 'Not supplied'}</span>
        </div>
        <div className="land-price-row">
          <span className="land-price">{listing.priceLabel}<small>{listing.unitPrice}</small></span>
          <span className="evidence-status"><FileSearch size={14} /> {listing.evidenceStatus}</span>
        </div>
        <div className="land-card-footer">
          <span className="broker-note">Source: broker-submitted · not independently verified</span>
          <Link to={`/lands/${listing.id}`} className="text-link">View details <ArrowUpRight size={15} /></Link>
        </div>
      </div>
    </article>
  )
}

export default LandCard
