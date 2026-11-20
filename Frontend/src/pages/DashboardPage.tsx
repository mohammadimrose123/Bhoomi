import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, ShieldCheck, Star, Ticket } from 'lucide-react'
import { dashboardApi, favoriteApi, siteVisitApi } from '../services/api'
import { useAuth } from '../context/AuthContext'

interface DashboardSummary {
  listingCount: number
  favoriteCount: number
  siteVisitCount: number
}

function DashboardPage() {
  const { user, isBroker, isAdmin } = useAuth()
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const load = async () => {
      try {
        setLoading(true)
        setError('')

        if (isAdmin) {
          setSummary(null)
          return
        }

        if (isBroker) {
          const [listingsResponse, visitsResponse] = await Promise.all([
            dashboardApi.brokerListings(),
            siteVisitApi.listBroker(),
          ])
          if (!Array.isArray(listingsResponse.data) || !Array.isArray(visitsResponse.data)) {
            throw new Error('Dashboard API returned an invalid response')
          }
          if (active) {
            setSummary({
              listingCount: listingsResponse.data.length,
              favoriteCount: 0,
              siteVisitCount: visitsResponse.data.length,
            })
          }
          return
        }

        const [favoritesResponse, visitsResponse] = await Promise.all([
          favoriteApi.list(),
          siteVisitApi.listBuyer(),
        ])
        if (!Array.isArray(favoritesResponse.data) || !Array.isArray(visitsResponse.data)) {
          throw new Error('Dashboard API returned an invalid response')
        }
        if (active) {
          setSummary({
            listingCount: 0,
            favoriteCount: favoritesResponse.data.length,
            siteVisitCount: visitsResponse.data.length,
          })
        }
      } catch (requestError) {
        if (active) {
          setSummary(null)
          setError('The dashboard could not load live data from the backend.')
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
  }, [isAdmin, isBroker])

  const title = isAdmin ? 'Administrator dashboard' : isBroker ? 'Broker dashboard' : 'Buyer dashboard'

  return (
    <section className="page-width" style={{ paddingTop: '2.5rem', paddingBottom: '4rem' }}>
      <div className="account-card" style={{ maxWidth: 980 }}>
        <p className="eyebrow">Account overview</p>
        <h1>{title}</h1>
        <p className="account-description">Signed in as {user?.fullName ?? user?.email} · {user?.role}</p>

        {error && <div className="account-error" role="alert">{error}</div>}

        {loading ? (
          <p className="empty-results">Loading dashboard data…</p>
        ) : summary && (
          <div className="land-grid explore-grid">
            <div className="land-card">
              <div className="land-card-body">
                <div className="land-card-topline">
                  <span className="category-tag">Listings</span>
                </div>
                <h3>{summary.listingCount}</h3>
                <p>{isBroker ? 'Broker-managed listings' : 'Saved listings on your account'}</p>
              </div>
            </div>
            <div className="land-card">
              <div className="land-card-body">
                <div className="land-card-topline">
                  <span className="category-tag">Favorites</span>
                </div>
                <h3>{summary.favoriteCount}</h3>
                <p>Saved land records</p>
              </div>
            </div>
            <div className="land-card">
              <div className="land-card-body">
                <div className="land-card-topline">
                  <span className="category-tag">Site visits</span>
                </div>
                <h3>{summary.siteVisitCount}</h3>
                <p>Requests tied to your account</p>
              </div>
            </div>
          </div>
        )}

        <div className="account-switch" style={{ marginTop: '1.5rem' }}>
          <Link to="/explore"><Star size={15} /> Explore land</Link>
          {user?.role === 'BUYER' && <Link to="/favorites" style={{ marginLeft: '1rem' }}><Star size={15} /> Favorites & activity</Link>}
          {user?.role === 'BUYER' && <Link to="/account" style={{ marginLeft: '1rem' }}><ShieldCheck size={15} /> Profile</Link>}
          {user?.role === 'BROKER' && (
            <Link to="/broker" style={{ marginLeft: '1rem' }}>
              <ShieldCheck size={15} /> Manage listings
            </Link>
          )}
          {user?.role === 'ADMIN' && <Link to="/admin" style={{ marginLeft: '1rem' }}><ShieldCheck size={15} /> Control center</Link>}
          <Link to="/" style={{ marginLeft: '1rem' }}>
            <Ticket size={15} /> Home
          </Link>
        </div>

        {user?.role === 'BROKER' && (
          <div className="demo-notice" style={{ marginTop: '1.5rem' }}>
            <span className="demo-notice-dot" />
            <span><strong>Broker access:</strong> listing and site-visit data is loaded from live backend APIs when available.</span>
          </div>
        )}

        <div style={{ marginTop: '1rem' }}>
          <Link className="button button-dark" to="/explore">Continue browsing <ArrowUpRight size={15} /></Link>
        </div>
      </div>
    </section>
  )
}

export default DashboardPage
