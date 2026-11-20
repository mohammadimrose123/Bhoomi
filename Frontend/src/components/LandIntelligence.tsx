import { ArrowUpRight, FileSearch, History, MapPinned, Route, Scale } from 'lucide-react'

const insights = [
  { icon: MapPinned, title: 'Survey context', body: 'Keep survey references and parcel details together, when supplied by the listing source.' },
  { icon: Route, title: 'Location context', body: 'See the listed locality and broker-supplied coordinates when available; map context is not a boundary survey.' },
  { icon: FileSearch, title: 'Supporting documents', body: 'Brokers can submit private documents for administrative review. Review is not title verification.' },
  { icon: History, title: 'Operational change audit', body: 'Status and asking-price changes are recorded for operational review, not presented as a verified transaction history.' },
  { icon: Scale, title: 'Price comparisons · planned', body: 'Comparable asking-price analysis is not available yet. Asking prices are not valuations or verified transaction prices.' },
]

function LandIntelligence() {
  return (
    <section className="intelligence-section" id="intelligence">
      <div className="page-width intelligence-layout">
        <div className="intelligence-intro">
          <p className="eyebrow eyebrow-light">More than a pin on a map</p>
          <h2>A better-informed<br /><em>way to look at land.</em></h2>
          <p>
            Bhoomi is designed to bring useful land details into one thoughtful view. A Land Passport is a record of shared information—not a title certificate or legal opinion.
          </p>
          <a className="intelligence-link" href="#how-it-works">How it works <ArrowUpRight size={16} /></a>
        </div>
        <div className="intelligence-list">
          {insights.map(({ icon: Icon, title, body }, index) => (
            <article className="intelligence-item" key={title}>
              <span className="intelligence-number">0{index + 1}</span>
              <span className="intelligence-icon"><Icon size={19} /></span>
              <div><h3>{title}</h3><p>{body}</p></div>
              <ArrowUpRight className="intelligence-item-arrow" size={16} />
            </article>
          ))}
          <p className="legal-note">Bhoomi does not verify legal title. Always consult a qualified legal professional before a transaction.</p>
        </div>
      </div>
    </section>
  )
}

export default LandIntelligence
