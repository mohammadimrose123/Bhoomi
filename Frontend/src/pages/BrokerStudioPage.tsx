import { useEffect, useState, type FormEvent } from 'react'
import { ArrowUpRight, BriefcaseBusiness, CalendarDays, ClipboardList, MessageCircle } from 'lucide-react'
import { Link } from 'react-router-dom'
import { brokerListingApi, brokerProfileApi, documentApi, enquiryApi, siteVisitApi } from '../services/api'

interface BrokerProfile {
  id: string
  businessName: string
  licenseNumber: string | null
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
}

interface BrokerListing {
  id: string
  title: string
  description: string
  askingPrice: number
  status: string
  sourceReference: string | null
  lastAvailabilityConfirmedAt: string | null
  media: Array<{ id: string; url: string; caption: string | null; mediaType: string }>
  parcel: {
    state: string
    district: string
    mandal: string
    village: string
    surveyNumber: string
    subdivisionNumber: string
    extent: number
    measurementUnit: string
    landType: string
    latitude: number | null
    longitude: number | null
  }
}

interface BrokerEnquiry {
  id: string
  listingId: string
  buyerId: string
  message: string
  contactPhone: string | null
  status: string
  createdAt: string
}

interface BrokerSiteVisit {
  id: string
  listingId: string
  buyerId: string
  requestedAt: string
  status: 'REQUESTED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'
  notes: string | null
}

interface BrokerDocument {
  id: string
  originalFilename: string
  contentType: string
  provenance: string
  reviewStatus: string
  description: string | null
}

interface ListingForm {
  title: string
  description: string
  askingPrice: string
  state: string
  district: string
  mandal: string
  village: string
  surveyNumber: string
  subdivisionNumber: string
  extent: string
  measurementUnit: string
  landType: string
  latitude: string
  longitude: string
  sourceReference: string
}

const emptyListing: ListingForm = {
  title: '',
  description: '',
  askingPrice: '',
  state: 'Telangana',
  district: '',
  mandal: '',
  village: '',
  surveyNumber: '',
  subdivisionNumber: '',
  extent: '',
  measurementUnit: 'ACRE',
  landType: 'AGRICULTURAL',
  latitude: '',
  longitude: '',
  sourceReference: '',
}

function BrokerStudioPage() {
  const [profile, setProfile] = useState<BrokerProfile | null>(null)
  const [listings, setListings] = useState<BrokerListing[]>([])
  const [enquiries, setEnquiries] = useState<BrokerEnquiry[]>([])
  const [visits, setVisits] = useState<BrokerSiteVisit[]>([])
  const [documents, setDocuments] = useState<Record<string, BrokerDocument[]>>({})
  const [selectedDocuments, setSelectedDocuments] = useState<Record<string, File | undefined>>({})
  const [selectedImages, setSelectedImages] = useState<Record<string, File | undefined>>({})
  const [businessName, setBusinessName] = useState('')
  const [licenseNumber, setLicenseNumber] = useState('')
  const [form, setForm] = useState<ListingForm>(emptyListing)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    const load = async () => {
      try {
        const [profileResponse, listingResponse, enquiryResponse, visitResponse] = await Promise.all([
          brokerProfileApi.get(),
          brokerListingApi.list(),
          enquiryApi.listBroker(),
          siteVisitApi.listBroker(),
        ])
        if (!active) return
        const brokerListings = listingResponse.data as BrokerListing[]
        const documentEntries = await Promise.all(brokerListings.map(async (listing) => {
          const response = await documentApi.listForListing(listing.id)
          return [listing.id, response.data as BrokerDocument[]] as const
        }))
        const nextProfile = profileResponse.data as BrokerProfile
        setProfile(nextProfile)
        setBusinessName(nextProfile.businessName)
        setLicenseNumber(nextProfile.licenseNumber ?? '')
        setListings(brokerListings)
        setDocuments(Object.fromEntries(documentEntries))
        setEnquiries(enquiryResponse.data as BrokerEnquiry[])
        setVisits(visitResponse.data as BrokerSiteVisit[])
      } catch {
        if (active) setError('Broker data could not be loaded from the backend.')
      } finally {
        if (active) setLoading(false)
      }
    }
    void load()
    return () => { active = false }
  }, [])

  const updateForm = (field: keyof ListingForm, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const saveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    try {
      const response = await brokerProfileApi.update({ businessName, licenseNumber: licenseNumber || undefined })
      setProfile(response.data as BrokerProfile)
      setSuccess('Broker profile updated.')
    } catch {
      setError('Broker profile could not be updated.')
    }
  }

  const saveListing = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (Boolean(form.latitude) !== Boolean(form.longitude)) {
      setError('Enter both latitude and longitude, or leave both empty.')
      return
    }
    setSaving(true)
    setError('')
    setSuccess('')
    try {
      if (editingId) {
        const response = await brokerListingApi.update(editingId, {
          title: form.title,
          description: form.description,
          askingPrice: Number(form.askingPrice),
        })
        setListings((current) => current.map((listing) => listing.id === editingId ? response.data as BrokerListing : listing))
        setSuccess('Listing changes saved.')
      } else {
        const response = await brokerListingApi.createDraft({
          title: form.title,
          description: form.description,
          askingPrice: Number(form.askingPrice),
          parcel: {
            state: form.state,
            district: form.district,
            mandal: form.mandal,
            village: form.village,
            surveyNumber: form.surveyNumber,
            subdivisionNumber: form.subdivisionNumber || undefined,
            extent: Number(form.extent),
            measurementUnit: form.measurementUnit,
            landType: form.landType,
            latitude: form.latitude ? Number(form.latitude) : undefined,
            longitude: form.longitude ? Number(form.longitude) : undefined,
          },
          sourceReference: form.sourceReference || undefined,
        })
        setListings((current) => [response.data as BrokerListing, ...current])
        setSuccess('Draft saved. Review the details and submit it when ready.')
      }
      setForm(emptyListing)
      setEditingId(null)
    } catch {
      setError('Listing could not be saved. Check required fields and try again.')
    } finally {
      setSaving(false)
    }
  }

  const editListing = (listing: BrokerListing) => {
    setEditingId(listing.id)
    setForm({
      ...emptyListing,
      title: listing.title,
      description: listing.description,
      askingPrice: String(listing.askingPrice),
      state: listing.parcel.state,
      district: listing.parcel.district,
      mandal: listing.parcel.mandal,
      village: listing.parcel.village,
      surveyNumber: listing.parcel.surveyNumber,
      subdivisionNumber: listing.parcel.subdivisionNumber,
      extent: String(listing.parcel.extent),
      measurementUnit: listing.parcel.measurementUnit,
      landType: listing.parcel.landType,
      latitude: listing.parcel.latitude == null ? '' : String(listing.parcel.latitude),
      longitude: listing.parcel.longitude == null ? '' : String(listing.parcel.longitude),
      sourceReference: listing.sourceReference ?? '',
    })
  }

  const submitListing = async (listingId: string) => {
    try {
      const response = await brokerListingApi.submit(listingId)
      setListings((current) => current.map((listing) => listing.id === listingId ? response.data as BrokerListing : listing))
      setSuccess('Listing submitted for review.')
      setError('')
    } catch {
      setError('Listing could not be submitted. Check broker approval and listing status.')
    }
  }

  const updateAvailability = async (listingId: string, status: 'SOLD' | 'UNAVAILABLE' | 'ARCHIVED') => {
    try {
      const response = await brokerListingApi.updateStatus(listingId, status)
      setListings((current) => current.map((listing) => listing.id === listingId ? response.data as BrokerListing : listing))
      setError('')
    } catch {
      setError('Listing availability could not be updated.')
    }
  }

  const confirmAvailability = async (listingId: string) => {
    try {
      const response = await brokerListingApi.confirmAvailability(listingId)
      setListings((current) => current.map((listing) => listing.id === listingId ? response.data as BrokerListing : listing))
      setSuccess('Listing availability confirmed.')
      setError('')
    } catch {
      setError('Availability could not be confirmed. Refresh and try again.')
    }
  }

  const uploadImage = async (listingId: string) => {
    const file = selectedImages[listingId]
    if (!file) {
      setError('Choose a PNG or JPEG image first.')
      return
    }
    try {
      const response = await brokerListingApi.uploadMedia(listingId, file)
      const uploaded = response.data as BrokerListing['media'][number]
      setListings((current) => current.map((listing) => listing.id === listingId
        ? { ...listing, media: [...(listing.media ?? []), uploaded] }
        : listing))
      setSelectedImages((current) => ({ ...current, [listingId]: undefined }))
      setSuccess('Listing image uploaded.')
      setError('')
    } catch {
      setError('Image upload failed. Only valid PNG or JPEG files up to 10 MB are accepted.')
    }
  }

  const updateVisit = async (visitId: string, status: BrokerSiteVisit['status']) => {
    try {
      const response = await siteVisitApi.updateStatus(visitId, status)
      setVisits((current) => current.map((visit) => visit.id === visitId ? response.data as BrokerSiteVisit : visit))
      setError('')
    } catch {
      setError('The site-visit request could not be updated.')
    }
  }

  const uploadDocument = async (listingId: string) => {
    const file = selectedDocuments[listingId]
    if (!file) {
      setError('Choose a PDF, PNG, or JPEG document first.')
      return
    }
    try {
      const response = await documentApi.upload(listingId, file)
      const uploaded = response.data as BrokerDocument
      setDocuments((current) => ({ ...current, [listingId]: [uploaded, ...(current[listingId] ?? [])] }))
      setSelectedDocuments((current) => ({ ...current, [listingId]: undefined }))
      setSuccess('Document uploaded for administrative review.')
      setError('')
    } catch {
      setError('Document upload failed. Only valid PDF, PNG, or JPEG files up to 10 MB are accepted.')
    }
  }

  const downloadDocument = async (document: BrokerDocument) => {
    try {
      const response = await documentApi.download(document.id)
      downloadBlob(response.data as Blob, document.originalFilename)
    } catch {
      setError('This private document could not be downloaded.')
    }
  }

  if (loading) return <section className="page-width not-found"><p className="eyebrow">Broker studio</p><h1>Loading your workspace…</h1></section>

  return (
    <section className="page-width" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      <div className="section-heading"><div><p className="eyebrow">Broker workspace</p><h1>Broker studio</h1></div><Link className="button button-outline" to="/explore">View public listings <ArrowUpRight size={15} /></Link></div>
      {error && <div className="account-error" role="alert">{error}</div>}
      {success && <div className="account-success" role="status">{success}</div>}

      <section className="passport-panel">
        <div className="passport-heading"><span><BriefcaseBusiness size={19} /></span><div><p className="eyebrow">Broker profile</p><h2>{profile?.approvalStatus ?? 'Status unavailable'}</h2></div></div>
        <form className="account-form" onSubmit={(event) => void saveProfile(event)}>
          <label>Business name<input value={businessName} onChange={(event) => setBusinessName(event.target.value)} maxLength={160} required /></label>
          <label>License number<input value={licenseNumber} onChange={(event) => setLicenseNumber(event.target.value)} maxLength={100} /></label>
          <button className="button button-dark" type="submit">Update profile</button>
        </form>
        <p className="legal-note">Broker approval is an administrative status. It is not a legal verification of a broker’s claims or a land title.</p>
      </section>

      <section className="passport-panel" style={{ marginTop: '1.5rem' }}>
        <div className="passport-heading"><span><ClipboardList size={19} /></span><div><p className="eyebrow">Inventory management</p><h2>{editingId ? 'Edit listing' : 'Create a listing draft'}</h2></div></div>
        <form className="account-form" onSubmit={(event) => void saveListing(event)}>
          <label>Listing title<input value={form.title} onChange={(event) => updateForm('title', event.target.value)} maxLength={180} required /></label>
          <label>Description<textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} maxLength={5000} rows={4} required /></label>
          <label>Asking price (INR)<input type="number" min="1" step="0.01" value={form.askingPrice} onChange={(event) => updateForm('askingPrice', event.target.value)} required /></label>
          <label>State<input value={form.state} onChange={(event) => updateForm('state', event.target.value)} maxLength={100} required /></label>
          <label>District<input value={form.district} onChange={(event) => updateForm('district', event.target.value)} maxLength={120} required /></label>
          <label>Mandal<input value={form.mandal} onChange={(event) => updateForm('mandal', event.target.value)} maxLength={120} required /></label>
          <label>Village<input value={form.village} onChange={(event) => updateForm('village', event.target.value)} maxLength={120} required /></label>
          <label>Survey number<input value={form.surveyNumber} onChange={(event) => updateForm('surveyNumber', event.target.value)} maxLength={80} required /></label>
          <label>Subdivision number<input value={form.subdivisionNumber} onChange={(event) => updateForm('subdivisionNumber', event.target.value)} maxLength={80} /></label>
          <label>Latitude (optional)<input type="number" min="-90" max="90" step="0.000001" value={form.latitude} onChange={(event) => updateForm('latitude', event.target.value)} /></label>
          <label>Longitude (optional)<input type="number" min="-180" max="180" step="0.000001" value={form.longitude} onChange={(event) => updateForm('longitude', event.target.value)} /></label>
          <label>Extent<input type="number" min="0.0001" step="0.0001" value={form.extent} onChange={(event) => updateForm('extent', event.target.value)} required /></label>
          <label>Measurement unit<select value={form.measurementUnit} onChange={(event) => updateForm('measurementUnit', event.target.value)}><option value="ACRE">Acre</option><option value="GUNTA">Gunta</option><option value="SQUARE_YARD">Square yard</option><option value="SQUARE_FOOT">Square foot</option><option value="HECTARE">Hectare</option></select></label>
          <label>Land category<select value={form.landType} onChange={(event) => updateForm('landType', event.target.value)}><option value="AGRICULTURAL">Agricultural</option><option value="RESIDENTIAL">Residential</option><option value="COMMERCIAL">Commercial</option><option value="INDUSTRIAL">Industrial</option><option value="MIXED_USE">Mixed use</option><option value="OTHER">Other</option></select></label>
          <label>Evidence/source reference (optional)<input value={form.sourceReference} onChange={(event) => updateForm('sourceReference', event.target.value)} maxLength={500} placeholder="Reference supplied by the broker" /></label>
          <small>Source references are user-supplied and are not independently validated by Bhoomi.</small>
          <div className="account-switch">
            <button className="button button-dark" type="submit" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Save changes' : 'Save draft'}</button>
            {editingId && <button className="button button-outline" type="button" onClick={() => { setEditingId(null); setForm(emptyListing) }}>Cancel edit</button>}
          </div>
        </form>
      </section>

      <section className="section">
        <div className="section-heading"><div><p className="eyebrow">Your inventory</p><h2>Listings</h2></div></div>
        {listings.length ? <div className="passport-items">{listings.map((listing) => (
          <article key={listing.id}>
            <span className="category-tag">{listing.status}</span>
            <h3>{listing.title}</h3>
            <p>{listing.parcel.village}, {listing.parcel.mandal}, {listing.parcel.district} · ₹{Number(listing.askingPrice).toLocaleString('en-IN')}</p>
            <p>{listing.parcel.latitude != null && listing.parcel.longitude != null
              ? `Coordinates: ${listing.parcel.latitude}, ${listing.parcel.longitude}`
              : 'Coordinates not supplied'}</p>
            <p>Source: broker-submitted{listing.sourceReference ? ` · ${listing.sourceReference}` : ''}</p>
            {listing.status === 'PUBLISHED' && (
              <div>
                <p>{availabilityReminder(listing.lastAvailabilityConfirmedAt)}</p>
                <button className="button button-outline" type="button" onClick={() => void confirmAvailability(listing.id)}>
                  Confirm still available
                </button>
              </div>
            )}
            <section aria-label={`Images for ${listing.title}`} className="passport-panel" style={{ marginTop: '1rem' }}>
              <p className="eyebrow">Listing photos</p>
              <input
                type="file"
                accept=".png,.jpg,.jpeg,image/png,image/jpeg"
                aria-label={`Choose a listing photo for ${listing.title}`}
                onChange={(event) => setSelectedImages((current) => ({ ...current, [listing.id]: event.target.files?.[0] }))}
              />
              <button className="button button-outline" type="button" onClick={() => void uploadImage(listing.id)}>
                Upload image
              </button>
              <p>{listing.media?.length ?? 0} image{listing.media?.length === 1 ? '' : 's'} attached</p>
            </section>
            <div className="account-switch">
              {(listing.status === 'DRAFT' || listing.status === 'REJECTED') && <button className="button button-outline" type="button" onClick={() => editListing(listing)}>Edit</button>}
              {(listing.status === 'DRAFT' || listing.status === 'REJECTED') && <button className="button button-dark" type="button" onClick={() => void submitListing(listing.id)}>Submit for review</button>}
              {listing.status === 'PUBLISHED' && <button className="button button-outline" type="button" onClick={() => void updateAvailability(listing.id, 'UNAVAILABLE')}>Mark unavailable</button>}
              <Link to={`/lands/${listing.id}`}>Open details <ArrowUpRight size={14} /></Link>
            </div>
            <section aria-label={`Documents for ${listing.title}`} className="passport-panel" style={{ marginTop: '1rem' }}>
              <p className="eyebrow">Private supporting documents</p>
              <input
                type="file"
                accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                aria-label={`Choose a document for ${listing.title}`}
                onChange={(event) => setSelectedDocuments((current) => ({ ...current, [listing.id]: event.target.files?.[0] }))}
              />
              <button className="button button-outline" type="button" onClick={() => void uploadDocument(listing.id)}>Upload document</button>
              {(documents[listing.id] ?? []).map((document) => (
                <p key={document.id}>
                  {document.originalFilename} · {document.reviewStatus} · {document.provenance}
                  <button className="filter-reset" type="button" onClick={() => void downloadDocument(document)}>Download</button>
                </p>
              ))}
              <small>Private files are available to the uploading broker and administrators. Review status is not proof of legal ownership.</small>
            </section>
          </article>
        ))}</div> : <p className="empty-results">No broker listings yet. Create a draft above.</p>}
      </section>

      <section className="passport-panel">
        <div className="passport-heading"><span><MessageCircle size={19} /></span><div><p className="eyebrow">Buyer conversations</p><h2>Enquiries</h2></div></div>
        {enquiries.length ? <div className="passport-items">{enquiries.map((enquiry) => (
          <article key={enquiry.id}>
            <span>Listing <Link to={`/lands/${enquiry.listingId}`}>{enquiry.listingId.slice(0, 8)}</Link> · Buyer {enquiry.buyerId.slice(0, 8)}</span>
            <strong>{enquiry.status}</strong><p>{enquiry.message}</p>
            {enquiry.contactPhone && <p>Buyer contact: {enquiry.contactPhone}</p>}
          </article>
        ))}</div> : <p className="empty-results">No buyer enquiries yet.</p>}
      </section>

      <section className="passport-panel" style={{ marginTop: '1.5rem' }}>
        <div className="passport-heading"><span><CalendarDays size={19} /></span><div><p className="eyebrow">Appointment management</p><h2>Site visits</h2></div></div>
        {visits.length ? <div className="passport-items">{visits.map((visit) => (
          <article key={visit.id}>
            <span>Listing <Link to={`/lands/${visit.listingId}`}>{visit.listingId.slice(0, 8)}</Link> · Buyer {visit.buyerId.slice(0, 8)}</span>
            <strong>{visit.status} · {new Date(visit.requestedAt).toLocaleString()}</strong>
            {visit.notes && <p>{visit.notes}</p>}
            <div className="account-switch">
              {visit.status === 'REQUESTED' && <button className="button button-outline" type="button" onClick={() => void updateVisit(visit.id, 'CONFIRMED')}>Confirm</button>}
              {visit.status === 'CONFIRMED' && <button className="button button-outline" type="button" onClick={() => void updateVisit(visit.id, 'COMPLETED')}>Mark completed</button>}
              {(visit.status === 'REQUESTED' || visit.status === 'CONFIRMED') && <button className="button button-outline" type="button" onClick={() => void updateVisit(visit.id, 'CANCELLED')}>Cancel request</button>}
            </div>
          </article>
        ))}</div> : <p className="empty-results">No site-visit requests yet.</p>}
      </section>
    </section>
  )
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

export default BrokerStudioPage

function availabilityReminder(confirmedAt: string | null) {
  if (!confirmedAt) return 'Availability has not been confirmed since publication.'
  const ageInDays = (Date.now() - new Date(confirmedAt).getTime()) / (24 * 60 * 60 * 1000)
  if (ageInDays >= 30) return 'Availability confirmation is more than 30 days old; please review it.'
  return `Last availability confirmation: ${new Date(confirmedAt).toLocaleDateString('en-IN')}`
}
