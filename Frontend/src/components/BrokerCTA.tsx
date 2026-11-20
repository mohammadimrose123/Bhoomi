import { ArrowUpRight, BriefcaseBusiness } from 'lucide-react'
import { Link } from 'react-router-dom'

function BrokerCTA() {
  return (
    <section className="broker-cta page-width">
      <div className="broker-cta-icon"><BriefcaseBusiness size={22} /></div>
      <div className="broker-cta-copy">
        <p className="eyebrow eyebrow-light">For brokers</p>
        <h2>Good land deserves<br />a clearer <em>introduction.</em></h2>
        <p>Prepare a listing with useful location details and supporting information for the people exploring it.</p>
      </div>
      <div className="broker-cta-action">
        <Link className="button button-gold" to="/register">Create a broker account <ArrowUpRight size={16} /></Link>
        <small>Broker listings can be submitted after administrative approval.</small>
      </div>
      <span className="broker-cta-decoration" aria-hidden="true">B</span>
    </section>
  )
}

export default BrokerCTA
