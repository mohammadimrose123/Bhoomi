import { ArrowUpRight, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'

const locations = [
  { name: 'Hyderabad', note: 'The city & its edges', image: 'photo-1570168007204-dfb528c6958f' },
  { name: 'Siddipet', note: 'North Telangana', image: 'photo-1464822759023-fed622ff2c3b' },
  { name: 'Gajwel', note: 'Siddipet district', image: 'photo-1511497584788-876760111969' },
  { name: 'Medchal', note: 'North of Hyderabad', image: 'photo-1441974231531-c6227db76b6e' },
  { name: 'Rangareddy', note: 'South & west of the city', image: 'photo-1500530855697-b586d89ba3ee' },
  { name: 'Sangareddy', note: 'Western Telangana', image: 'photo-1501785888041-af3ef285b470' },
]

function LocationSection() {
  return (
    <section className="section page-width">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Rooted in the region</p>
          <h2>Start with a <em>place.</em></h2>
        </div>
        <p className="section-intro">Explore Telangana one locality at a time, with district and village context built into every search.</p>
      </div>
      <div className="location-grid">
        {locations.map((location, index) => (
          <Link
            className={`location-card${index === 0 ? ' location-card-featured' : ''}`}
            to={`/explore?district=${encodeURIComponent(
              location.name === 'Gajwel'
                ? 'Siddipet'
                : location.name === 'Medchal'
                  ? 'Medchal-Malkajgiri'
                  : location.name,
            )}`}
            key={location.name}
          >
            <img
              src={`https://images.unsplash.com/${location.image}?auto=format&fit=crop&w=1100&q=80`}
              alt=""
              loading="lazy"
              onError={(event) => { event.currentTarget.style.visibility = 'hidden' }}
            />
            <span className="location-shade" />
            <span className="location-content">
              <small><MapPin size={13} /> {location.note}</small>
              <strong>{location.name}</strong>
            </span>
            <span className="location-arrow"><ArrowUpRight size={16} /></span>
          </Link>
        ))}
      </div>
      <div className="all-districts">
        <span>Looking somewhere else in Telangana?</span>
        <Link to="/explore">Explore all districts <ArrowUpRight size={15} /></Link>
      </div>
    </section>
  )
}

export default LocationSection
