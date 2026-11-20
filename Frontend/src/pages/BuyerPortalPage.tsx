import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, Heart, MessageCircle, UserRound, CalendarDays } from 'lucide-react'
import { buyerProfileApi, enquiryApi, favoriteApi, siteVisitApi } from '../services/api'
import { fetchLandListingById } from '../services/landService'
import type { LandListing } from '../types/land'

interface BuyerProfile {
  id: string
  email: string
  fullName: string
  phone: string | null
}

interface EnquiryRecord {
  id: string
  listingId: string
  message: string
  status: string
  createdAt: string
}

interface SiteVisitRecord {
  id: string
  listingId: string
  requestedAt: string
  status: string
  notes: string | null
}

function BuyerPortalPage() {
  const [profile, setProfile] = useState<BuyerProfile | null>(null)
  const [favorites, setFavorites] = useState<LandListing[]>([])
  const [enquiries, setEnquiries] = useState<EnquiryRecord[]>([])
  const [visits, setVisits] = useState<SiteVisitRecord[]>([])
  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(true)
  const [savingProfile, setSavingProfile] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true

    const load = async () => {
      try {
        setLoading(true)
        const [profileResponse, favoriteResponse, enquiryResponse, visitResponse] = await Promise.all([
          buyerProfileApi.get(),
          favoriteApi.list(),
          enquiryApi.listBuyer(),
          siteVisitApi.listBuyer(),
        ])
        const listingIds: string[] = (favoriteResponse.data as Array<{ listingId: string }>).map((favorite) => favorite.listingId)
        const favoriteListings = await Promise.all(listingIds.map(fetchLandListingById))

        if (active) {
          const currentProfile = profileResponse.data as BuyerProfile
          setProfile(currentProfile)
          setFullName(currentProfile.fullName)
          setPhone(currentProfile.phone ?? '')
          setFavorites(favoriteListings)
          setEnquiries(enquiryResponse.data as EnquiryRecord[])
          setVisits(visitResponse.data as SiteVisitRecord[])
        }
      } catch {
        if (active) setError('Your account data could not be loaded from the backend. Please try again.')
      } finally {
        if (active) setLoading(false)
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [])

  const saveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSavingProfile(true)
    setError('')
    setSuccess('')
    try {
      const response = await buyerProfileApi.update({ fullName, phone: phone || undefined })
      setProfile(response.data as BuyerProfile)
      setSuccess('Your profile was updated.')
    } catch {
      setError('Your profile could not be updated. Check the information and try again.')
    } finally {
      setSavingProfile(false)
    }
  }

  const removeFavorite = async (listingId: string) => {
    setError('')
    try {
      await favoriteApi.remove(listingId)
      setFavorites((current) => current.filter((listing) => listing.id !== listingId))
    } catch {
      setError('This favorite could not be removed. Please try again.')
    }
  }

  if (loading) {
    return <section className="page-width not-found"><p className="eyebrow">Buyer account</p><h1>Loading your account…</h1></section>
  }

  return (
    <section className="page-width" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      <div className="section-heading">
        <div><p className="eyebrow">Your Bhoomi account</p><h1>Buyer portal</h1></div>
        <Link className="button button-outline" to="/explore">Explore land <ArrowUpRight size={15} /></Link>
      </div>
      {error && <div className="account-error" role="alert">{error}</div>}
      {success && <div className="account-success" role="status">{success}</div>}

      <section className="passport-panel" aria-labelledby="buyer-profile-title">
        <div className="passport-heading"><span><UserRound size={19} /></span><div><p className="eyebrow">Profile</p><h2 id="buyer-profile-title">Contact information</h2></div></div>
        <form className="account-form" onSubmit={saveProfile}>
          <label>Full name<input value={fullName} onChange={(event) => setFullName(event.target.value)} minLength={2} maxLength={160} required /></label>
          <label>Email address<input type="email" value={profile?.email ?? ''} readOnly /></label>
          <label>Phone number<input type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} maxLength={32} /></label>
          <button className="button button-dark" type="submit" disabled={savingProfile}>{savingProfile ? 'Saving…' : 'Save profile'}</button>
        </form>
      </section>

      <section className="section" aria-labelledby="buyer-favorites-title">
        <div className="section-heading"><div><p className="eyebrow"><Heart size={14} /> Saved land</p><h2 id="buyer-favorites-title">Your favorites</h2></div></div>
        {favorites.length ? (
          <div className="land-grid explore-grid">
            {favorites.map((listing) => (
              <article className="land-card" key={listing.id}>
                <div className="land-card-body">
                  <span className="category-tag">{listing.category}</span>
                  <h3><Link to={`/lands/${listing.id}`}>{listing.title}</Link></h3>
                  <p>{listing.village}, {listing.mandal}, {listing.district}</p>
                  <p className="land-price">{listing.priceLabel}</p>
                  <button className="filter-reset" type="button" onClick={() => void removeFavorite(listing.id)}>Remove favorite</button>
                </div>
              </article>
            ))}
          </div>
        ) : <p className="empty-results">You have not saved any listings yet. Browse live inventory to add favorites.</p>}
      </section>

      <section className="passport-panel" aria-labelledby="buyer-enquiries-title">
        <div className="passport-heading"><span><MessageCircle size={19} /></span><div><p className="eyebrow">Broker conversations</p><h2 id="buyer-enquiries-title">Enquiry history</h2></div></div>
        {enquiries.length ? (
          <div className="passport-items">
            {enquiries.map((enquiry) => (
              <div key={enquiry.id}>
                <span>Listing <Link to={`/lands/${enquiry.listingId}`}>{enquiry.listingId.slice(0, 8)}</Link></span>
                <strong>{enquiry.status}</strong>
                <p>{enquiry.message}</p>
                <small>{new Date(enquiry.createdAt).toLocaleString()}</small>
              </div>
            ))}
          </div>
        ) : <p className="empty-results">No enquiries yet.</p>}
      </section>

      <section className="passport-panel" aria-labelledby="buyer-visits-title" style={{ marginTop: '1.5rem' }}>
        <div className="passport-heading"><span><CalendarDays size={19} /></span><div><p className="eyebrow">Property visits</p><h2 id="buyer-visits-title">Site-visit requests</h2></div></div>
        {visits.length ? (
          <div className="passport-items">
            {visits.map((visit) => (
              <div key={visit.id}>
                <span>Listing <Link to={`/lands/${visit.listingId}`}>{visit.listingId.slice(0, 8)}</Link></span>
                <strong>{visit.status}</strong>
                <p>{new Date(visit.requestedAt).toLocaleString()}</p>
                {visit.notes && <small>{visit.notes}</small>}
              </div>
            ))}
          </div>
        ) : <p className="empty-results">No site visits have been requested.</p>}
      </section>
    </section>
  )
}

export default BuyerPortalPage
