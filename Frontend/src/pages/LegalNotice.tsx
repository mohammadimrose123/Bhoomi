import { Link, useLocation } from 'react-router-dom'

function LegalNotice() {
  const isPrivacy = useLocation().pathname === '/privacy'
  const title = isPrivacy ? 'Privacy policy' : 'Terms & conditions'

  return (
    <section className="page-width legal-page">
      <p className="eyebrow">Service information</p>
      <h1>{title}</h1>
      <p>This service is in development. Account, enquiry, favorite, site-visit, and broker document forms can send information to the Bhoomi backend when it is available.</p>
      <p>Listing and parcel details are supplied by brokers and are not government records or proof of legal ownership. Do not treat a document review status as title verification.</p>
      <p>This notice is not the final {isPrivacy ? 'privacy policy' : 'terms of use'}; approved legal terms and privacy disclosures are required before public launch.</p>
      <Link to="/" className="text-link">Return to home</Link>
    </section>
  )
}

export default LegalNotice
