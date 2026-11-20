import { ArrowRight, Compass, FileText, MessageCircle, Signpost } from 'lucide-react'

const steps = [
  { icon: Compass, number: '01', title: 'Discover land', body: 'Search by district, locality, category and the budget you have in mind.' },
  { icon: FileText, number: '02', title: 'Review what’s shared', body: 'Look through broker-submitted parcel details and the information available for each listing.' },
  { icon: MessageCircle, number: '03', title: 'Connect with a broker', body: 'Ask for the information you need before deciding what comes next.' },
  { icon: Signpost, number: '04', title: 'Plan a site visit', body: 'See the land in person and complete your own independent due diligence.' },
]

function HowItWorks() {
  return (
    <section className="section page-width" id="how-it-works">
      <div className="section-heading">
        <div>
          <p className="eyebrow">A considered process</p>
          <h2>From first search to <em>first visit.</em></h2>
        </div>
      </div>
      <div className="steps-grid">
        {steps.map(({ icon: Icon, number, title, body }, index) => (
          <article className="step-card" key={number}>
            <div className="step-top"><span>{number}</span><Icon size={21} />{index < steps.length - 1 && <ArrowRight className="step-connector" size={17} />}</div>
            <h3>{title}</h3>
            <p>{body}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

export default HowItWorks
