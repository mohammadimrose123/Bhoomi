import { ArrowLeft } from 'lucide-react'
import { Link } from 'react-router-dom'

function NotFound() {
  return (
    <section className="page-width not-found">
      <p className="eyebrow">Page not found</p>
      <h1>This page isn’t available.</h1>
      <Link className="button button-dark" to="/"><ArrowLeft size={16} /> Back to Bhoomi</Link>
    </section>
  )
}

export default NotFound
