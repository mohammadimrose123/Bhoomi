import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowUpRight, FileSearch, MapPin, Route } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { fetchLandListingById } from '../services/landService'
import { enquiryApi, siteVisitApi } from '../services/api'
import type { LandListing } from '../types/land'

function LandDetails() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated, isBuyer } = useAuth()
  const [listing, setListing] = useState<LandListing | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [requestShown, setRequestShown] = useState(false)
  const [enquiryShown, setEnquiryShown] = useState(false)
  const [enquiryMessage, setEnquiryMessage] = useState('')
  const [requestedAt, setRequestedAt] = useState('')
  const [notes, setNotes] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submittingEnquiry, setSubmittingEnquiry] = useState(false)
  const [unavailableImage, setUnavailableImage] = useState<string>()
  const minimumVisitTime = toLocalDateTimeInput(new Date(Date.now() + 60_000))

  useEffect(() => {
    if (!id) return

    let active = true

    const load = async () => {
      try {
        setLoading(true)
        const details = await fetchLandListingById(id)
        if (active) setListing(details)
      } catch {
        if (active) setError('This listing could not be loaded from the backend.')
      } finally {
        if (active) setLoading(false)
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [id])

  const handleSiteVisitRequest = async () => {
    if (!id) return
    if (!isAuthenticated || !isBuyer) {
      navigate('/login', { state: { from: `/lands/${id}` } })
      return
    }

    if (!notes.trim()) {
      setError('Please add a short note for the broker before requesting a site visit.')
      return
    }

    if (!requestedAt || new Date(requestedAt).getTime() <= Date.now()) {
      setError('Choose a future date and time for the site visit.')
      return
    }

    setSubmitting(true)
    try {
      await siteVisitApi.create(id, new Date(requestedAt).toISOString(), notes)
      setRequestShown(true)
      setRequestedAt('')
      setNotes('')
      setError('')
    } catch {
      setError('Your site-visit request could not be submitted. Please try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleEnquiry = async () => {
    if (!id) return
    if (!isAuthenticated || !isBuyer) {
      navigate('/login', { state: { from: `/lands/${id}` } })
      return
    }
    if (enquiryMessage.trim().length < 10) {
      setError('Please enter at least 10 characters for your enquiry.')
      return
    }

    setSubmittingEnquiry(true)
    try {
      await enquiryApi.create(id, enquiryMessage.trim())
      setEnquiryShown(true)
      setEnquiryMessage('')
      setError('')
    } catch {
      setError('Your enquiry could not be submitted. Please try again.')
    } finally {
      setSubmittingEnquiry(false)
    }
  }

  if (loading) {
    return (
      <section className="page-width not-found">
        <p className="eyebrow">Loading listing</p>
        <h1>Fetching the live land record…</h1>
      </section>
    )
  }

  if (error || !listing) {
    return (
      <section className="page-width not-found">
        <p className="eyebrow">Listing not found</p>
        <h1>This listing isn’t available.</h1>
        <Link className="button button-dark" to="/explore"><ArrowLeft size={16} /> Back to explore</Link>
      </section>
    )
  }

  return (
    <div className="page-width details-page">
      <div className="breadcrumb"><Link to="/">Home</Link><span>/</span><Link to="/explore">Explore</Link><span>/</span><span>{listing.title}</span></div>
      <div className="details-image">
        {unavailableImage !== listing.image && (
          <img
            src={listing.image}
            alt={`Landscape representing ${listing.category.toLowerCase()}`}
            onError={() => setUnavailableImage(listing.image)}
          />
        )}
        <span className="demo-label">Published listing</span>
        {listing.imageIsRepresentative && <span className="image-attribution">Representative image, not parcel photography</span>}
      </div>
      <div className="details-layout">
        <div className="details-main">
          <span className="category-tag">{listing.category}</span>
          <h1>{listing.title}</h1>
          <p className="details-location"><MapPin size={17} /> {listing.village}, {listing.mandal}, {listing.district}, Telangana</p>
          {listing.description && <p className="details-description">{listing.description}</p>}
          <div className="details-facts">
            <div><small>Land extent</small><strong>{listing.extent}</strong></div>
            <div><small>Survey number</small><strong>{listing.surveyNumber ?? 'Not supplied'}</strong></div>
            <div><small>Asking price</small><strong>{listing.priceLabel}</strong></div>
            <div><small>Price reference</small><strong>{listing.unitPrice}</strong></div>
          </div>
          <section className="passport-panel">
            <div className="passport-heading"><span><FileSearch size={19} /></span><div><p className="eyebrow">Land Passport</p><h2>Shared information at a glance</h2></div></div>
            <p>This listing is supplied by the remote Bhoomi backend and is subject to the platform’s review conventions. It does not represent a legal title verification.</p>
            <div className="passport-items">
              <div><MapPin size={16} /><span>Location context</span><strong>{listing.district}</strong></div>
              <div><Route size={16} /><span>Survey reference</span><strong>{listing.surveyNumber ?? 'Not supplied'}</strong></div>
              {listing.sourceReference && (
                <div><FileSearch size={16} /><span>Broker-supplied source reference</span><strong>{listing.sourceReference}</strong></div>
              )}
              <div><FileSearch size={16} /><span>Document evidence</span><strong>{listing.evidenceStatus}</strong></div>
              {listing.latitude != null && listing.longitude != null && (
                <div>
                  <MapPin size={16} />
                  <span>Broker-supplied coordinates</span>
                  <a
                    href={`https://www.openstreetmap.org/?mlat=${listing.latitude}&mlon=${listing.longitude}#map=16/${listing.latitude}/${listing.longitude}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    View on OpenStreetMap
                  </a>
                </div>
              )}
            </div>
          </section>
          <p className="legal-note detail-legal">Bhoomi does not verify legal title. Any real transaction requires independent checks and advice from a qualified legal professional.</p>
        </div>
        <aside className="details-contact">
          <p className="eyebrow">Asking price</p>
          <strong className="details-price">{listing.priceLabel}</strong>
          <span className="details-unit-price">{listing.unitPrice}</span>
          <div className="contact-divider" />

          {isAuthenticated && isBuyer ? (
            <>
              <label className="filter-control" style={{ marginBottom: '1rem' }}>
                Ask the broker
                <textarea
                  value={enquiryMessage}
                  onChange={(event) => setEnquiryMessage(event.target.value)}
                  placeholder="Ask a question about this land (at least 10 characters)."
                  rows={4}
                  maxLength={2000}
                />
              </label>
              <button type="button" className="button button-outline contact-request" onClick={handleEnquiry} disabled={submittingEnquiry}>
                {submittingEnquiry ? 'Sending…' : 'Send an enquiry'} <ArrowUpRight size={16} />
              </button>
              {enquiryShown && <p className="preview-message" role="status">Your enquiry has been sent to the listing broker.</p>}
              <label className="filter-control" style={{ marginBottom: '1rem' }}>
                Requested date and time
                <input
                  type="datetime-local"
                  value={requestedAt}
                  min={minimumVisitTime}
                  onChange={(event) => setRequestedAt(event.target.value)}
                  required
                />
              </label>
              <label className="filter-control" style={{ marginBottom: '1rem' }}>
                Visit notes
                <textarea
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                  placeholder="Tell the broker when you would like to inspect the parcel."
                  rows={4}
                />
              </label>
              <button type="button" className="button button-dark contact-request" onClick={handleSiteVisitRequest} disabled={submitting}>
                {submitting ? 'Submitting…' : 'Request a site visit'} <ArrowUpRight size={16} />
              </button>
            </>
          ) : (
            <button type="button" className="button button-dark contact-request" onClick={() => navigate('/login', { state: { from: `/lands/${id}` } })}>
              Sign in to request a visit <ArrowUpRight size={16} />
            </button>
          )}

          {requestShown && <p className="preview-message" role="status">Your site-visit request has been submitted.</p>}
          {error && <p className="account-error" role="alert">{error}</p>}
          <Link className="back-link" to="/explore"><ArrowLeft size={15} /> Back to listings</Link>
        </aside>
      </div>
    </div>
  )
}

function toLocalDateTimeInput(date: Date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 16)
}

export default LandDetails
