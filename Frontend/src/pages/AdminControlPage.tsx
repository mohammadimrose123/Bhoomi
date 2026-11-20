import { useEffect, useState } from 'react'
import { ArrowUpRight, BadgeCheck, ClipboardCheck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { adminApi, dashboardApi } from '../services/api'

interface BrokerQueueItem {
  id: string
  userId: string
  businessName: string
  licenseNumber: string | null
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
}

interface ReviewListing {
  id: string
  title: string
  description: string
  askingPrice: number
  status: 'SUBMITTED' | 'UNDER_REVIEW'
  parcel: { district: string; mandal: string; village: string; surveyNumber: string }
}

interface AdminDocument {
  id: string
  landParcelId: string
  originalFilename: string
  provenance: string
  reviewStatus: string
  description: string | null
  createdAt: string
}

function AdminControlPage() {
  const [brokers, setBrokers] = useState<BrokerQueueItem[]>([])
  const [listings, setListings] = useState<ReviewListing[]>([])
  const [documents, setDocuments] = useState<AdminDocument[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const [brokerResponse, listingResponse, documentResponse] = await Promise.all([
        adminApi.brokerQueue(),
        dashboardApi.adminReviews(),
        adminApi.documents(),
      ])
      setBrokers(brokerResponse.data as BrokerQueueItem[])
      setListings(listingResponse.data as ReviewListing[])
      setDocuments(documentResponse.data as AdminDocument[])
    } catch {
      setError('Administrative queues could not be loaded from the backend.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  const reviewBroker = async (id: string, status: 'APPROVED' | 'REJECTED') => {
    try {
      await adminApi.updateBrokerStatus(id, status, `Broker ${status.toLowerCase()} by administrator`)
      setSuccess(`Broker ${status.toLowerCase()}.`)
      await load()
    } catch {
      setError('Broker status could not be updated.')
    }
  }

  const reviewListing = async (id: string, status: 'UNDER_REVIEW' | 'PUBLISHED' | 'REJECTED') => {
    try {
      await adminApi.updateListingStatus(id, status, `Listing ${status.toLowerCase()} by administrator`)
      setSuccess(`Listing moved to ${status.toLowerCase().replace('_', ' ')}.`)
      await load()
    } catch {
      setError('Listing status could not be updated. Check that the broker is approved before publishing.')
    }
  }

  const reviewDocument = async (id: string, status: 'REVIEWED' | 'REJECTED') => {
    try {
      await adminApi.reviewDocument(id, status, status === 'REVIEWED' ? 'Document reviewed; not legal title verification' : 'Document rejected during administrative review')
      setSuccess(`Document marked ${status.toLowerCase()}.`)
      await load()
    } catch {
      setError('Document review status could not be updated.')
    }
  }

  const downloadDocument = async (documentRecord: AdminDocument) => {
    try {
      const response = await adminApi.downloadDocument(documentRecord.id)
      downloadBlob(response.data as Blob, documentRecord.originalFilename)
    } catch {
      setError('Private document download failed.')
    }
  }

  return (
    <section className="page-width" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      <div className="section-heading"><div><p className="eyebrow">Administration</p><h1>Control center</h1></div><Link className="button button-outline" to="/dashboard">Account overview <ArrowUpRight size={15} /></Link></div>
      {error && <div className="account-error" role="alert">{error}</div>}
      {success && <div className="account-success" role="status">{success}</div>}
      {loading ? <p className="empty-results">Loading moderation queues…</p> : (
        <>
          <section className="passport-panel">
            <div className="passport-heading"><span><BadgeCheck size={19} /></span><div><p className="eyebrow">Broker moderation</p><h2>Pending approvals ({brokers.length})</h2></div></div>
            {brokers.length ? <div className="passport-items">{brokers.map((broker) => (
              <article key={broker.id}>
                <strong>{broker.businessName}</strong>
                <p>Broker profile ID: {broker.id} · User ID: {broker.userId}</p>
                <p>License reference: {broker.licenseNumber || 'Not supplied'} · Status: {broker.approvalStatus}</p>
                <div className="account-switch">
                  <button className="button button-dark" type="button" onClick={() => void reviewBroker(broker.id, 'APPROVED')}>Approve</button>
                  <button className="button button-outline" type="button" onClick={() => void reviewBroker(broker.id, 'REJECTED')}>Reject</button>
                </div>
              </article>
            ))}</div> : <p className="empty-results">No brokers are waiting for review.</p>}
          </section>

          <section className="passport-panel" style={{ marginTop: '1.5rem' }}>
            <div className="passport-heading"><span><ClipboardCheck size={19} /></span><div><p className="eyebrow">Listing moderation</p><h2>Review queue ({listings.length})</h2></div></div>
            {listings.length ? <div className="passport-items">{listings.map((listing) => (
              <article key={listing.id}>
                <span className="category-tag">{listing.status}</span>
                <h3>{listing.title}</h3>
                <p>{listing.parcel.village}, {listing.parcel.mandal}, {listing.parcel.district} · Survey {listing.parcel.surveyNumber}</p>
                <p>{listing.description}</p>
                <p>Broker asking price: ₹{Number(listing.askingPrice).toLocaleString('en-IN')} · Listing ID: {listing.id}</p>
                <div className="account-switch">
                  {listing.status === 'SUBMITTED' && <button className="button button-outline" type="button" onClick={() => void reviewListing(listing.id, 'UNDER_REVIEW')}>Mark under review</button>}
                  <button className="button button-dark" type="button" onClick={() => void reviewListing(listing.id, 'PUBLISHED')}>Publish</button>
                  <button className="button button-outline" type="button" onClick={() => void reviewListing(listing.id, 'REJECTED')}>Reject</button>
                </div>
              </article>
            ))}</div> : <p className="empty-results">No listings are waiting for moderation.</p>}
            <p className="legal-note">Administrative publication is a marketplace review, not proof of legal ownership or title.</p>
          </section>

          <section className="passport-panel" style={{ marginTop: '1.5rem' }}>
            <div className="passport-heading"><span><ClipboardCheck size={19} /></span><div><p className="eyebrow">Evidence administration</p><h2>Documents awaiting review ({documents.length})</h2></div></div>
            {documents.length ? <div className="passport-items">{documents.map((documentRecord) => (
              <article key={documentRecord.id}>
                <strong>{documentRecord.originalFilename}</strong>
                <p>Parcel {documentRecord.landParcelId} · {documentRecord.provenance} · {new Date(documentRecord.createdAt).toLocaleString()}</p>
                {documentRecord.description && <p>{documentRecord.description}</p>}
                <div className="account-switch">
                  <button className="button button-outline" type="button" onClick={() => void downloadDocument(documentRecord)}>Download private file</button>
                  <button className="button button-dark" type="button" onClick={() => void reviewDocument(documentRecord.id, 'REVIEWED')}>Mark reviewed</button>
                  <button className="button button-outline" type="button" onClick={() => void reviewDocument(documentRecord.id, 'REJECTED')}>Reject</button>
                </div>
              </article>
            ))}</div> : <p className="empty-results">No documents are waiting for review.</p>}
            <p className="legal-note">Review indicates administrative handling only; it is not legal title verification or proof of ownership.</p>
          </section>
        </>
      )}
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

export default AdminControlPage
