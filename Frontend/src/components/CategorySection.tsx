import { ArrowUpRight, Building2, Factory, Flower2, Sprout } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ComponentType } from 'react'
import type { LucideProps } from 'lucide-react'

const categories: {
  name: string
  tagline: string
  image: string
  icon: ComponentType<LucideProps>
}[] = [
  { name: 'Agricultural land', tagline: 'For cultivation & investment', image: 'photo-1500382017468-9049fed747ef', icon: Sprout },
  { name: 'Residential plots', tagline: 'Space to make your own', image: 'photo-1500076656116-558758c991c1', icon: Building2 },
  { name: 'Commercial land', tagline: 'Room for new ventures', image: 'photo-1486406146926-c627a92ad1ab', icon: Factory },
  { name: 'Industrial land', tagline: 'Explore strategic locations', image: 'photo-1516937941344-00b4e0337589', icon: Factory },
  { name: 'Mixed-use land', tagline: 'A flexible starting point', image: 'photo-1470071459604-3b5ec3a7fe05', icon: Sprout },
  { name: 'Other land', tagline: 'Explore a broader range', image: 'photo-1510798831971-661eb04b3739', icon: Building2 },
]

function CategorySection() {
  return (
    <section className="section page-width" id="categories">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Find your kind of ground</p>
          <h2>Land for every <em>next step.</em></h2>
        </div>
        <p className="section-intro">Start with the kind of place you have in mind. We’ll help you explore the details that shape it.</p>
      </div>
      <div className="category-grid">
        {categories.map(({ name, tagline, image, icon: Icon }) => (
          <Link
            className="category-card"
            key={name}
            to={`/explore?category=${encodeURIComponent(name)}`}
            aria-label={`Explore ${name}`}
          >
            <img
              src={`https://images.unsplash.com/${image}?auto=format&fit=crop&w=900&q=80`}
              alt=""
              loading="lazy"
              onError={(event) => { event.currentTarget.style.visibility = 'hidden' }}
            />
            <span className="category-overlay" />
            <span className="category-icon"><Icon size={18} /></span>
            <span className="category-copy">
              <span>{name}</span>
              <small>{tagline}</small>
            </span>
            <span className="category-arrow"><ArrowUpRight size={17} /></span>
          </Link>
        ))}
      </div>
      <p className="category-footnote"><Flower2 size={14} /> Categories filter published listings; imagery is illustrative.</p>
    </section>
  )
}

export default CategorySection
